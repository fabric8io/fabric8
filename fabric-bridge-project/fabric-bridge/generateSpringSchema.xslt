<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="@use">
		<!-- skip the use attribute -->
	</xsl:template>

    <xsl:template match="node()[@name='propertySet']">
        <!-- skip the element with the name propertySet -->
    </xsl:template>

    <xsl:template match="node()[@name='exportedConnectionFactory']">
        <!-- skip the element with the name exportedConnectionFactory -->
    </xsl:template>

    <xsl:template match="node()[@name='exportedMessageConverter']">
        <!-- skip the element with the name exportedMessageConverter -->
    </xsl:template>

	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>
