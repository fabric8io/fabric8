@echo off
setlocal 

if NOT "%JAVA_APP_DIR%"=="" goto CHECK_SETENV

REM Discover JAVA_APP_DIR from the script's location.
pushd .  
cd %~dp0..
set "JAVA_APP_DIR=%CD%"

:CHECK_SETENV
if EXIST %JAVA_APP_DIR%\bin\setenv.cmd call %JAVA_APP_DIR%\bin\setenv.cmd

if "%JAVA_MAIN_CLASS%"=="" (
	if "${hawtapp.mvn.main.property}"=="" (
	  echo "No JAVA_MAIN_CLASS specified"
  	  goto :EOF
	) else (
		set JAVA_MAIN_CLASS=${hawtapp.mvn.main.property}
	)
)


:CHECK_JAVA_CLASSPATH
if NOT "%JAVA_CLASSPATH%"=="" goto CHECK_DEBUG
for /F "tokens=*" %%A in ( %JAVA_APP_DIR%\lib\classpath ) do (
  CALL SET JAVA_CLASSPATH=%%JAVA_CLASSPATH%%;%JAVA_APP_DIR%\lib\%%A
)

SET JAVA_CLASSPATH=%JAVA_CLASSPATH:~1%

:CHECK_DEBUG
REM Set debug options if required
if NOT "%JAVA_ENABLE_DEBUG%"=="" (
  if NOT "%JAVA_ENABLE_DEBUG%"=="false" (
    set java_debug_args=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
  )
)


REM Compose the command line
set arg_list= %java_debug_args% %JAVA_OPTIONS% -classpath "%JAVA_CLASSPATH%" %JAVA_MAIN_CLASS%
if NOT "%JAVA_MAIN_ARGS%"=="" (
	set arg_list=%arg_list% %JAVA_MAIN_ARGS%
) else (
	set arg_list=%arg_list% %*
)

echo Launching application in folder: %JAVA_APP_DIR%
cd %JAVA_APP_DIR%

echo Running java %arg_list%
java %arg_list%

:EOF