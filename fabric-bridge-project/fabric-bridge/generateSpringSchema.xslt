<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.

        http://fusesource.com

    The software in this package is published under the terms of the
    CDDL license a copy of which has been included with this distribution
    in the license.txt file.

-->
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
