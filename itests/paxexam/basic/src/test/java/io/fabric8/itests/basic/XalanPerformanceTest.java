/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.itests.basic;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import io.fabric8.itests.paxexam.support.FabricTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class XalanPerformanceTest extends FabricTestSupport {

    private String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<root>\n" +
        "    <child>value</child>\n" +
        "</root>\n";

    @Test
    public void testXPath() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document document = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(this.xml)));

        long ms1 = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            Element e = (Element)document.getElementsByTagName("child").item(0);
            e.getTextContent();
        }
        System.out.println("T1: " + (System.currentTimeMillis() - ms1) + "ms");

        XPath xp = XPathFactory.newInstance().newXPath();
        XPathExpression expression = xp.compile("/root/child/text()");
        long ms2 = System.currentTimeMillis();
        // see: http://java.dzone.com/articles/how-speed-apache-xalan%E2%80%99s-xpath
        // 4443ms without optimisation
        // 2397ms with optimisation
        for (int i = 0; i < 10000; i++) {
            expression.evaluate(new InputSource(new StringReader(this.xml)), XPathConstants.STRING);
        }
        System.out.println("T2: " + (System.currentTimeMillis() - ms2) + "ms");
    }

    @Configuration
    public Option[] config() {
        return new Option[] {
            new DefaultCompositeOption(fabricDistributionConfiguration()),
            KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "org.apache.xml.dtm.DTMManager", "org.apache.xml.dtm.ref.DTMManagerDefault"),
            KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "com.sun.org.apache.xml.internal.dtm.DTMManager", "com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault")
        };
    }

}
