<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright (C) FuseSource, Inc.
  http://fusesource.com

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
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
