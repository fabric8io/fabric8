<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.

        http://fusesource.com

    The software in this package is published under the terms of the
    CDDL license a copy of which has been included with this distribution
    in the license.txt file.

-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:import href="../fabric-bridge/generateSpringSchema.xslt"/>

    <xsl:template match="@schemaLocation">
        <xsl:attribute name="schemaLocation">http://fusesource.org/fabric/bridge/fabric-bridge.xsd</xsl:attribute>
    </xsl:template>

</xsl:stylesheet>
