/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.dns;

import io.fabric8.api.Container;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Closeables;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Credibility;
import org.xbill.DNS.DClass;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TSIGRecord;
import org.xbill.DNS.Type;
import org.xbill.DNS.Zone;
import org.xbill.DNS.ZoneTransferException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

@Component(configurationPid = "io.fabric8.dns.zone", policy = ConfigurationPolicy.OPTIONAL, immediate = true)
@Service(FabricZoneManager.class)
public class FabricZoneManager implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricZoneManager.class);

    private static final int FLAG_DNSSECOK = 1;
    private static final int FLAG_SIGONLY = 2;

    private static final long MINUTE = 60;
    private static final long HOUR = 3600;
    private static final long DAY = 86400;
    private static final long WEEK = 604800;

    private final Map<Integer, Cache> caches = new HashMap<>();
    private final Map<Name, Zone> znames = new HashMap<>();
    private final Map<Name, TSIG> TSIGs = new HashMap<>();

    @Property(name = "domain", label = "Domain", description = "The Fabric8 domain name", value = "fabric8.local")
    private String domain;

    @Property(name = "container.sub.domain", label = "Container Sub Domain", description = "The Fabric8 container sub domain", value = "container")
    private String containerSubDomain;

    @Property(name = "service.sub.domain", label = "Service Sub Domain", description = "The Fabric8 service sub domain", value = "service")
    private String serviceSubDomain;

    @Property(name = "nameServer", label = "Name Server", description = "The name server of the fabric domain", value = "ns")
    private String nameServer;

    @Property(name = "adminServer", label = "Admin", description = "The adminServer of the fabric domain", value = "admin")
    private String adminServer;

    @Property(name = "refresh", label = "Refresh", description = "The amount of time until a secondary checks for a new serial number", longValue = HOUR)
    private long refresh = HOUR;

    @Property(name = "retry", label = "Retry", description = "The amount of time between a secondary's checks for a new serial number", longValue = HOUR)
    private long retry = HOUR;

    @Property(name = "expire", label = "Expire", description = "The amount of time until a secondary expires a zone", longValue = HOUR)
    private long expire = HOUR;

    @Property(name = "minimum.ttl", label = "Minimum TTL", description = "The minimum TTL for records in the zone", longValue = HOUR)
    private long minimumTtl = HOUR;

    @Property(name = "groups", label = "Groups", description = "The groups to expose via DNS SRV queries", cardinality = -1, value = {"git"})
    private List<String> groups;

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<>();


    private Name domainRoot;
    private Name containerDomain;
    private Name serivceDomain;
    private Name ns;
    private Name admin;
    private Zone fabricZone;
    private final GroupListener<SrvNode> groupListener = new GroupServiceListener();
    private final Map<String, Group> activeGroups = new HashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Activate
    void activate(Map<String,?> config) throws Exception {
        configurer.configure(config, this);
        domainRoot = Name.fromString(domain, Name.root);
        containerDomain = Name.fromString(containerSubDomain, domainRoot);
        serivceDomain = Name.fromString(serviceSubDomain, domainRoot);
        ns = Name.fromString(nameServer, domainRoot);
        admin = Name.fromString(adminServer, domainRoot);
        fabricZone = createFabricZone();
        znames.put(fabricZone.getOrigin(), fabricZone);
        monitorGroups();
        executor.scheduleAtFixedRate(this, 30, 30, TimeUnit.SECONDS);
    }

    @Deactivate
    void deactivate() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        for (Group group : activeGroups.values()) {
            group.remove(groupListener);
            Closeables.closeQuitely(group);
        }
        caches.clear();
        znames.clear();
        TSIGs.clear();
    }


    @Override
    public void run() {
        try {
            updateContainers();
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    private Zone createFabricZone() throws IOException, ZoneTransferException {
        Name domainRoot = Name.fromString(domain, Name.root);
        Name containerDomain = Name.fromString(containerSubDomain, domainRoot);
        Name serivceDomain = Name.fromString(serviceSubDomain, domainRoot);
        Name ns = Name.fromString(nameServer, domainRoot);
        Name admin = Name.fromString(adminServer, domainRoot);

        FabricService service = fabricService.get();
        List<Record> records = new ArrayList<>();
        //TODO: At some point we need to manage the serial number.
        records.add(new SOARecord(domainRoot, DClass.IN, DAY, ns, admin, 1, refresh, retry, expire, minimumTtl));
        records.add(new NSRecord(domainRoot, DClass.IN, DAY, ns));
        return new Zone(domainRoot, records.toArray(new Record[records.size()]));
    }

    private synchronized void updateContainers() throws Exception {
        //Create A Records for Containers
        for (Container container : fabricService.get().getContainers()) {
            String id = container.getId();
            String address = container.getIp();
            Record record = new ARecord(Name.fromString(id, containerDomain), DClass.IN, minimumTtl, InetAddress.getByName(address));
            fabricZone.addRecord(record);
        }
    }

    private void monitorGroups() throws Exception {
        //Create SRV Records for Services
        CuratorFramework curator = fabricService.get().adapt(CuratorFramework.class);
        for (String groupName : groups) {
            Group<SrvNode> group = activeGroups.get(groupName);
            if (group == null) {
                group = new ZooKeeperGroup(curator, "/fabric/registry/clusters/" + groupName, SrvNode.class);
                group.add(groupListener);
                group.start();
                activeGroups.put(groupName, group);
            }
        }
    }


    public Cache getCache(int dclass) {
        Cache c = (Cache) caches.get(new Integer(dclass));
        if (c == null) {
            c = new Cache(dclass);
            caches.put(new Integer(dclass), c);
        }
        return c;
    }

    public Zone findBestZone(Name name) {
        Zone foundzone = null;
        foundzone = (Zone) znames.get(name);
        if (foundzone != null)
            return foundzone;
        int labels = name.labels();
        for (int i = 1; i < labels; i++) {
            Name tname = new Name(name, i);
            foundzone = (Zone) znames.get(tname);
            if (foundzone != null)
                return foundzone;
        }
        return null;
    }

    public RRset findExactMatch(Name name, int type, int dclass, boolean glue) {
        Zone zone = findBestZone(name);
        if (zone != null)
            return zone.findExactMatch(name, type);
        else {
            RRset[] rrsets;
            Cache cache = getCache(dclass);
            if (glue)
                rrsets = cache.findAnyRecords(name, type);
            else
                rrsets = cache.findRecords(name, type);
            if (rrsets == null)
                return null;
            else
                return rrsets[0]; /* not quite right */
        }
    }

    void addRRset(Name name, Message response, RRset rrset, int section, int flags) {
        for (int s = 1; s <= section; s++)
            if (response.findRRset(name, rrset.getType(), s))
                return;
        if ((flags & FLAG_SIGONLY) == 0) {
            Iterator it = rrset.rrs();
            while (it.hasNext()) {
                Record r = (Record) it.next();
                if (r.getName().isWild() && !name.isWild())
                    r = r.withName(name);
                response.addRecord(r, section);
            }
        }
        if ((flags & (FLAG_SIGONLY | FLAG_DNSSECOK)) != 0) {
            Iterator it = rrset.sigs();
            while (it.hasNext()) {
                Record r = (Record) it.next();
                if (r.getName().isWild() && !name.isWild())
                    r = r.withName(name);
                response.addRecord(r, section);
            }
        }
    }

    private final void addSOA(Message response, Zone zone) {
        response.addRecord(zone.getSOA(), Section.AUTHORITY);
    }

    private final void addNS(Message response, Zone zone, int flags) {
        RRset nsRecords = zone.getNS();
        addRRset(nsRecords.getName(), response, nsRecords,
                Section.AUTHORITY, flags);
    }

    private final void addCacheNS(Message response, Cache cache, Name name) {
        SetResponse sr = cache.lookupRecords(name, Type.NS, Credibility.HINT);
        if (!sr.isDelegation())
            return;
        RRset nsRecords = sr.getNS();
        Iterator it = nsRecords.rrs();
        while (it.hasNext()) {
            Record r = (Record) it.next();
            response.addRecord(r, Section.AUTHORITY);
        }
    }

    private void addGlue(Message response, Name name, int flags) {
        RRset a = findExactMatch(name, Type.A, DClass.IN, true);
        if (a == null)
            return;
        addRRset(name, response, a, Section.ADDITIONAL, flags);
    }

    private void addAdditional2(Message response, int section, int flags) {
        Record[] records = response.getSectionArray(section);
        for (int i = 0; i < records.length; i++) {
            Record r = records[i];
            Name glueName = r.getAdditionalName();
            if (glueName != null)
                addGlue(response, glueName, flags);
        }
    }

    private final void addAdditional(Message response, int flags) {
        addAdditional2(response, Section.ANSWER, flags);
        addAdditional2(response, Section.AUTHORITY, flags);
    }

    byte addAnswer(Message response, Name name, int type, int dclass,
              int iterations, int flags) {
        SetResponse sr;
        byte rcode = Rcode.NOERROR;

        if (iterations > 6)
            return Rcode.NOERROR;

        if (type == Type.SIG || type == Type.RRSIG) {
            type = Type.ANY;
            flags |= FLAG_SIGONLY;
        }

        Zone zone = findBestZone(name);
        if (zone != null)
            sr = zone.findRecords(name, type);
        else {
            Cache cache = getCache(dclass);
            sr = cache.lookupRecords(name, type, Credibility.NORMAL);
        }

        if (sr.isUnknown()) {
            addCacheNS(response, getCache(dclass), name);
        }
        if (sr.isNXDOMAIN()) {
            response.getHeader().setRcode(Rcode.NXDOMAIN);
            if (zone != null) {
                addSOA(response, zone);
                if (iterations == 0)
                    response.getHeader().setFlag(Flags.AA);
            }
            rcode = Rcode.NXDOMAIN;
        } else if (sr.isNXRRSET()) {
            if (zone != null) {
                addSOA(response, zone);
                if (iterations == 0)
                    response.getHeader().setFlag(Flags.AA);
            }
        } else if (sr.isDelegation()) {
            RRset nsRecords = sr.getNS();
            addRRset(nsRecords.getName(), response, nsRecords,
                    Section.AUTHORITY, flags);
        } else if (sr.isCNAME()) {
            CNAMERecord cname = sr.getCNAME();
            RRset rrset = new RRset(cname);
            addRRset(name, response, rrset, Section.ANSWER, flags);
            if (zone != null && iterations == 0)
                response.getHeader().setFlag(Flags.AA);
            rcode = addAnswer(response, cname.getTarget(),
                    type, dclass, iterations + 1, flags);
        } else if (sr.isDNAME()) {
            DNAMERecord dname = sr.getDNAME();
            RRset rrset = new RRset(dname);
            addRRset(name, response, rrset, Section.ANSWER, flags);
            Name newname;
            try {
                newname = name.fromDNAME(dname);
            } catch (NameTooLongException e) {
                return Rcode.YXDOMAIN;
            }
            rrset = new RRset(new CNAMERecord(name, dclass, 0, newname));
            addRRset(name, response, rrset, Section.ANSWER, flags);
            if (zone != null && iterations == 0)
                response.getHeader().setFlag(Flags.AA);
            rcode = addAnswer(response, newname, type, dclass,
                    iterations + 1, flags);
        } else if (sr.isSuccessful()) {
            RRset[] rrsets = sr.answers();
            for (int i = 0; i < rrsets.length; i++)
                addRRset(name, response, rrsets[i],
                        Section.ANSWER, flags);
            if (zone != null) {
                addNS(response, zone, flags);
                if (iterations == 0)
                    response.getHeader().setFlag(Flags.AA);
            } else
                addCacheNS(response, getCache(dclass), name);
        }
        return rcode;
    }

    byte[] doAXFR(Name name, Message query, TSIG tsig, TSIGRecord qtsig, Socket s) {
        Zone zone = (Zone) znames.get(name);
        boolean first = true;
        if (zone == null)
            return errorMessage(query, Rcode.REFUSED);
        Iterator it = zone.AXFR();
        try {
            DataOutputStream dataOut;
            dataOut = new DataOutputStream(s.getOutputStream());
            int id = query.getHeader().getID();
            while (it.hasNext()) {
                RRset rrset = (RRset) it.next();
                Message response = new Message(id);
                Header header = response.getHeader();
                header.setFlag(Flags.QR);
                header.setFlag(Flags.AA);
                addRRset(rrset.getName(), response, rrset,
                        Section.ANSWER, FLAG_DNSSECOK);
                if (tsig != null) {
                    tsig.applyStream(response, qtsig, first);
                    qtsig = response.getTSIG();
                }
                first = false;
                byte[] out = response.toWire();
                dataOut.writeShort(out.length);
                dataOut.write(out);
            }
        } catch (IOException ex) {
            System.out.println("AXFR failed");
        }
        try {
            s.close();
        } catch (IOException ex) {
        }
        return null;
    }

    /*
     * Note: a null return value means that the caller doesn't need to do
     * anything.  Currently this only happens if this is an AXFR request over
     * TCP.
     */
    byte[] generateReply(Message query, byte[] in, int length, Socket s)
            throws IOException {
        Header header;
        boolean badversion;
        int maxLength;
        int flags = 0;

        header = query.getHeader();
        if (header.getFlag(Flags.QR))
            return null;
        if (header.getRcode() != Rcode.NOERROR)
            return errorMessage(query, Rcode.FORMERR);
        if (header.getOpcode() != Opcode.QUERY)
            return errorMessage(query, Rcode.NOTIMP);

        Record queryRecord = query.getQuestion();

        TSIGRecord queryTSIG = query.getTSIG();
        TSIG tsig = null;
        if (queryTSIG != null) {
            tsig = (TSIG) TSIGs.get(queryTSIG.getName());
            if (tsig == null ||
                    tsig.verify(query, in, length, null) != Rcode.NOERROR)
                return formerrMessage(in);
        }

        OPTRecord queryOPT = query.getOPT();
        if (queryOPT != null && queryOPT.getVersion() > 0)
            badversion = true;

        if (s != null)
            maxLength = 65535;
        else if (queryOPT != null)
            maxLength = Math.max(queryOPT.getPayloadSize(), 512);
        else
            maxLength = 512;

        if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0)
            flags = FLAG_DNSSECOK;

        Message response = new Message(query.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        if (query.getHeader().getFlag(Flags.RD))
            response.getHeader().setFlag(Flags.RD);
        response.addRecord(queryRecord, Section.QUESTION);

        Name name = queryRecord.getName();
        int type = queryRecord.getType();
        int dclass = queryRecord.getDClass();
        if (type == Type.AXFR && s != null)
            return doAXFR(name, query, tsig, queryTSIG, s);
        if (!Type.isRR(type) && type != Type.ANY)
            return errorMessage(query, Rcode.NOTIMP);

        byte rcode = addAnswer(response, name, type, dclass, 0, flags);
        if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN)
            return errorMessage(query, rcode);

        addAdditional(response, flags);

        if (queryOPT != null) {
            int optflags = (flags == FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
            OPTRecord opt = new OPTRecord((short) 4096, rcode, (byte) 0,
                    optflags);
            response.addRecord(opt, Section.ADDITIONAL);
        }

        response.setTSIG(tsig, Rcode.NOERROR, queryTSIG);
        return response.toWire(maxLength);
    }

    byte[] buildErrorMessage(Header header, int rcode, Record question) {
        Message response = new Message();
        response.setHeader(header);
        for (int i = 0; i < 4; i++)
            response.removeAllRecords(i);
        if (rcode == Rcode.SERVFAIL)
            response.addRecord(question, Section.QUESTION);
        header.setRcode(rcode);
        return response.toWire();
    }

    public byte[] formerrMessage(byte[] in) {
        Header header;
        try {
            header = new Header(in);
        } catch (IOException e) {
            return null;
        }
        return buildErrorMessage(header, Rcode.FORMERR, null);
    }

    public byte[] errorMessage(Message query, int rcode) {
        return buildErrorMessage(query.getHeader(), rcode,
                query.getQuestion());
    }


    private class GroupServiceListener implements GroupListener<SrvNode> {
        @Override
        public void groupEvent(Group<SrvNode> group, GroupEvent event) {
                switch (event) {
                    case CONNECTED:
                    case CHANGED:
                        for (Map.Entry<String, SrvNode> entry : group.members().entrySet()) {
                            SrvNode node = entry.getValue();
                            String containerId = node.getContainer();
                            int priority=0;
                            for (String srv : node.getServices()) {
                                try {
                                    String substitutedUrl = getSubstitutedData(fabricService.get().adapt(CuratorFramework.class), srv);
                                    URL serviceUrl = new URL(substitutedUrl);
                                    String groupName = node.getId();
                                    fabricZone.addRecord(new SRVRecord(Name.fromString(groupName, serivceDomain), DClass.IN, minimumTtl, priority++, 0, serviceUrl.getPort(), (Name.fromString(containerId, containerDomain))));
                                } catch (Exception e) {
                                    //ignore service that are not valid URLs.
                                }
                            }
                        }
                        break;
                    default:
                        // do nothing
                }
            }
    }

    void bindFabricService(FabricService service) {
        this.fabricService.bind(service);
    }

    void unbindFabricService(FabricService service) {
        this.fabricService.unbind(service);
    }

    void bindConfigurer(Configurer service) {
        this.configurer = service;
    }

    void unbindConfigurer(Configurer service) {
        this.configurer = null;
    }
}
