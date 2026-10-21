$envFile = "C:\Users\yoges\Documents\Codex\2026-10-07\as-you-are-a-proffesional-software\outputs\ChatFusion\backend\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^\w+=' } | ForEach-Object {
        $name, $value = $_ -split '=', 2
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}
$env:JAVA_HOME = 'C:\Users\yoges\.jdks\jbr-21.0.11'
$javaExe = "$env:JAVA_HOME\bin\java.exe"
$cp = "C:\Users\yoges\Documents\Codex\2026-10-07\as-you-are-a-proffesional-software\outputs\ChatFusion\backend\build\install\backend\lib\*"
Set-Location "C:\Users\yoges\Documents\Codex\2026-10-07\as-you-are-a-proffesional-software\outputs\ChatFusion\backend"
& $javaExe -cp $cp com.yogesh.chatfusion.ApplicationKt
