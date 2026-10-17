$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$cache=Join-Path $env:USERPROFILE '.m2/repository'
$groups=@('org/springframework','com/baomidou','org/mybatis','com/fasterxml/jackson','org/apache/tomcat','javax/servlet','org/slf4j')
$jars=foreach($group in $groups){Get-ChildItem -LiteralPath (Join-Path $cache $group) -Filter '*.jar' -Recurse -ErrorAction SilentlyContinue | ForEach-Object {$_.FullName}}
Set-Location -LiteralPath $root
$classpath='target/classes;'+($jars -join ';')
$java='C:/Program Files/Java/jdk-17.0.1/bin'
$output=Join-Path $root 'target/console-checks'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$source=Join-Path $root 'src/test/java/com/backend/com/minzu/controller/ConsoleBoundaryCheck.java'
$arguments='-encoding UTF-8 -cp "'+$classpath.Replace('\','/')+'" -d target/console-checks src/test/java/com/backend/com/minzu/controller/ConsoleBoundaryCheck.java src/test/java/com/backend/com/minzu/controller/AgentWorkflowCheck.java'
$argumentFile=Join-Path $output 'javac.args'
[System.IO.File]::WriteAllText($argumentFile,$arguments,[System.Text.UTF8Encoding]::new($false))
& (Join-Path $java 'javac.exe') ('@'+$argumentFile)
if($LASTEXITCODE -ne 0){throw 'Boundary test compilation failed'}
& (Join-Path $java 'java.exe') '-cp' ('target/console-checks;'+$classpath) 'com.backend.com.minzu.controller.ConsoleBoundaryCheck'
if($LASTEXITCODE -ne 0){throw 'Boundary tests failed'}
& (Join-Path $java 'java.exe') '-cp' ('target/console-checks;'+$classpath) 'com.backend.com.minzu.controller.AgentWorkflowCheck'
if($LASTEXITCODE -ne 0){throw 'Agent workflow checks failed'}
