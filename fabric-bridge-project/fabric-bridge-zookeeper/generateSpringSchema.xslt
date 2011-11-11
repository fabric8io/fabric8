<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:import href="../fabric-bridge/generateSpringSchema.xslt"/>

    <xsl:template match="@schemaLocation">
        <xsl:attribute name="schemaLocation">http://fusesource.org/fabric/bridge/fabric-bridge.xsd</xsl:attribute>
    </xsl:template>

</xsl:stylesheet>
