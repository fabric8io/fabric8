@echo off

rem
rem  Copyright 2005-2014 Red Hat, Inc.
rem
rem  Red Hat licenses this file to you under the Apache License, version
rem  2.0 (the "License"); you may not use this file except in compliance
rem  with the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem  Unless required by applicable law or agreed to in writing, software
rem  distributed under the License is distributed on an "AS IS" BASIS,
rem  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
rem  implied.  See the License for the specific language governing
rem  permissions and limitations under the License.
rem

if not "%ECHO%" == "" echo %ECHO%

setlocal
set DIRNAME=%~dp0%

rem   lets enable stand alone mode

set FABRIC8_ENSEMBLE_AUTO_START = false
set FABRIC8_AGENT_AUTO_START = false

call "%DIRNAME%\karaf.bat" %*
