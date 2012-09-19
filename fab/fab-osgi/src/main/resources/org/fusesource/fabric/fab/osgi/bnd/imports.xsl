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
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:beans="http://www.springframework.org/schema/beans">

    <xsl:output method="text" xml:space="preserve" />

    <xsl:template match="beans:import">
      <xsl:value-of xml:space="preserve" select="@resource"/>
      <xsl:text xml:space="preserve">
      </xsl:text>
    </xsl:template>

    <!-- stripping text nodes -->
    <xsl:template match="text()" />

</xsl:stylesheet>