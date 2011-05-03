/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.generator;

import org.fusesource.fusemq.amqp.jaxb.schema.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class AmqpDoc {

    List<Doc> docs = new LinkedList<Doc>();
    String label;

    AmqpDoc() {

    }

    AmqpDoc(Doc doc) {
        parseFromDoc(doc);
    }

    AmqpDoc(List<Doc> docs) {
        parseFromDoc(docs);
    }

    public void parseFromDoc(Doc doc) {

        this.docs.add(doc);
    }

    public void parseFromDoc(List<Doc> docs) {

        this.docs.addAll(docs);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void writeJavaDoc(BufferedWriter writer, int indent) throws IOException {
        String comment = "";
        if (label != null) {
            comment = label + "\n";
        }
        for (Doc doc : docs) {
            for (Object docType : doc.getDocOrPOrUlOrOlOrDlOrPicture()) {

                if (docType instanceof P) {
                    comment += handleP((P) docType);
                } else if (docType instanceof Ul) {
                    comment += handleUl((Ul) docType);
                } else if (docType instanceof Ol) {
                    comment += handleOl((Ol) docType);
                } else if (docType instanceof Dl) {
                    comment += handleDl((Dl) docType);
                } else if (docType instanceof Picture) {
                    comment += "\n<pre>\n" + ((Picture) docType).getvalue() + "</pre>\n";
                }
            }
            comment += "\n";
        }

        if (comment.length() > 0) {
            Utils.writeJavaComment(writer, indent, Utils.convertToLines(comment, 80));
        }
    }

    private static final String handleP(P p) {
        return "<p>\n" + p.getvalue() + "\n</p>\n";
    }

    private static final String handleUl(Ul ul) {
        String comment = "";
        comment += "<ul>\n";
        for (Li li : ul.getLi()) {
            comment += "<li>\n";
            for (Object uType : li.getPOrUl()) {
                if (uType instanceof P) {
                    comment += handleP((P) uType);
                } else {
                    comment += handleUl((Ul) uType);
                }
            }
            comment += "<\n></li>\n";
        }
        comment += "<\n></ul>\n";
        return comment;
    }

    private static final String handleOl(Ol ol) {
        String comment = "";
        comment += "<ol>\n";
        for (Li li : ol.getLi()) {
            comment += "<li>\n";
            for (Object uType : li.getPOrUl()) {
                if (uType instanceof P) {
                    comment += handleP((P) uType);
                } else {
                    comment += handleUl((Ul) uType);
                }
            }
            comment += "\n</li>\n";
        }
        comment += "\n</ol>\n";
        return comment;
    }

    private static final String handleDl(Dl dl) {
        String comment = "";
        comment += "<dl>\n";
        for (Object dType : dl.getDtOrDd()) {
            if (dType instanceof Dt) {
                comment += "<dt>\n" + ((Dt) dType).getvalue() + "\n</dt>\n";
            } else {
                for (P p : ((Dd) dType).getP()) {
                    comment += handleP(p);
                }
            }
        }
        comment += "\n</dl>\n";
        return comment;
    }

}
