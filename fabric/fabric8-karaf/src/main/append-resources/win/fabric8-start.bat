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
set PROGNAME=%~nx0%
set ARGS=%*

rem   lets check if we are not about to join a fabric
if "%FABRIC8_ZOOKEEPER_URL%" == "" (

  rem   unless we explicitly disable it, lets auto-start an ensemble
  if "%FABRIC8_AGENT_AUTO_START%" == "" set FABRIC8_ENSEMBLE_AUTO_START = true

  rem   unless we explicitly disable it, lets auto-start the agent
  if "%FABRIC8_AGENT_AUTO_START%" == "" set FABRIC8_AGENT_AUTO_START = true
)

rem Sourcing environment settings for karaf similar to tomcats setenv
SET KARAF_SCRIPT="start.bat"
if exist "%DIRNAME%setenv.bat" (
  call "%DIRNAME%setenv.bat"
)

rem Check console window title. Set to Karaf by default
if not "%KARAF_TITLE%" == "" (
    title %KARAF_TITLE%
) else (
    title Karaf
)

goto BEGIN

:warn
    echo %PROGNAME%: %*
goto :EOF

:BEGIN

rem # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

if not "%KARAF_HOME%" == "" (
    call :warn Ignoring predefined value for KARAF_HOME
)
set KARAF_HOME=%DIRNAME%..
if not exist "%KARAF_HOME%" (
    call :warn KARAF_HOME is not valid: "%KARAF_HOME%"
    goto END
)

if not "%KARAF_BASE%" == "" (
    if not exist "%KARAF_BASE%" (
       call :warn KARAF_BASE is not valid: "%KARAF_BASE%"
       goto END
    )
)
if "%KARAF_BASE%" == "" (
  set "KARAF_BASE=%KARAF_HOME%"
)

if not "%KARAF_DATA%" == "" (
    if not exist "%KARAF_DATA%" (
        call :warn KARAF_DATA is not valid: "%KARAF_DATA%"
        goto END
    )
)
if "%KARAF_DATA%" == "" (
    set "KARAF_DATA=%KARAF_BASE%\data"
)

if not "%KARAF_ETC%" == "" (
    if not exist "%KARAF_ETC%" (
        call :warn KARAF_ETC is not valid: "%KARAF_ETC%"
        goto END
    )
)
if "%KARAF_ETC%" == "" (
    set "KARAF_ETC=%KARAF_BASE%\etc"
)

:EXECUTE
    start "Karaf" /MIN "%KARAF_HOME%\bin\karaf.bat" server %*

rem # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

:END

endlocal

if not "%PAUSE%" == "" pause

:END_NO_PAUSE
