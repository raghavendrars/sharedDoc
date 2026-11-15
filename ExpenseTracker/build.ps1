# Builds dist\ExpenseTracker.apk.
#   .\build.ps1                          -> app is called "Expense Tracker" on the phone
#   .\build.ps1 -AppName "Home Kharcha"  -> any name you like under the icon
param([string]$AppName = "Expense Tracker")

$ErrorActionPreference = "Stop"
$root  = $PSScriptRoot
$tools = "D:\games\android-tools"
$sdk   = "$tools\sdk"
$bt    = "$sdk\build-tools\34.0.0"
$jar   = "$sdk\platforms\android-34\android.jar"
$jdkDir = Get-ChildItem "$tools\jdk" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1

if (-not $jdkDir) { throw "JDK not found under $tools\jdk" }
if (-not (Test-Path "$bt\aapt2.exe") -or -not (Test-Path $jar)) { throw "Android build tools are missing. Run setup-sdk.cmd once first." }
$jdk = $jdkDir.FullName
$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"

function Run([string]$what, [scriptblock]$cmd) {
    Write-Host "== $what"
    & $cmd
    if ($LASTEXITCODE -ne 0) { throw "$what failed (exit code $LASTEXITCODE)" }
}

$b = "$root\build"
if (Test-Path $b) { Remove-Item $b -Recurse -Force -Confirm:$false }
New-Item -ItemType Directory -Force "$b\gen", "$b\classes", "$b\dex", "$root\dist" | Out-Null

# Resources, with the chosen app name written into strings.
Copy-Item "$root\app\res" "$b\res" -Recurse
$valuesFile = "$b\res\values\values.xml"
$safeName = [System.Security.SecurityElement]::Escape($AppName).Replace("'", "\'")
$xml = [System.IO.File]::ReadAllText($valuesFile)
$xml = [regex]::Replace($xml, '<string name="app_name">.*?</string>', { param($m) "<string name=`"app_name`">$safeName</string>" })
[System.IO.File]::WriteAllText($valuesFile, $xml, (New-Object System.Text.UTF8Encoding($false)))

Run "Compile resources" { & "$bt\aapt2.exe" compile --dir "$b\res" -o "$b\res.zip" }
Run "Link resources" {
    & "$bt\aapt2.exe" link -o "$b\base.apk" -I $jar --manifest "$root\app\AndroidManifest.xml" --java "$b\gen" `
        --min-sdk-version 26 --target-sdk-version 34 --version-code 1 --version-name "1.0" "$b\res.zip"
}

$sources = Get-ChildItem "$root\app\src", "$b\gen" -Recurse -Filter *.java | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
[System.IO.File]::WriteAllLines("$b\sources.txt", $sources, (New-Object System.Text.UTF8Encoding($false)))
Run "Compile Java" { & "$jdk\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath $jar -d "$b\classes" "@$b\sources.txt" }
Run "Package classes" { & "$jdk\bin\jar.exe" cf "$b\classes.jar" -C "$b\classes" . }
Run "Convert to dex" { & "$bt\d8.bat" --release --lib $jar --min-api 26 --output "$b\dex" "$b\classes.jar" }

Push-Location "$b\dex"
try { Run "Add code to APK" { & "$bt\aapt.exe" add "$b\base.apk" classes.dex | Out-Null } } finally { Pop-Location }
Run "Align" { & "$bt\zipalign.exe" -f -p 4 "$b\base.apk" "$b\aligned.apk" }

# Signing key: created once and reused, so later builds install as updates over the old one. Keep the keystore folder safe.
$ksDir = "$root\keystore"
$ks = "$ksDir\expense-tracker.jks"
$pwFile = "$ksDir\password.txt"
if (-not (Test-Path $ks)) {
    New-Item -ItemType Directory -Force $ksDir | Out-Null
    $pw = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
    [System.IO.File]::WriteAllText($pwFile, $pw)
    Run "Create signing key" {
        & "$jdk\bin\keytool.exe" -genkeypair -keystore $ks -alias expense -keyalg RSA -keysize 2048 -validity 10000 `
            -storepass $pw -keypass $pw -dname "CN=Expense Tracker"
    }
}
$pw = [System.IO.File]::ReadAllText($pwFile).Trim()
$apk = "$root\dist\ExpenseTracker.apk"
Run "Sign" { & "$bt\apksigner.bat" sign --ks $ks --ks-key-alias expense --ks-pass "pass:$pw" --key-pass "pass:$pw" --out $apk "$b\aligned.apk" }
Run "Verify" { & "$bt\apksigner.bat" verify $apk }

$size = [math]::Round((Get-Item $apk).Length / 1KB)
Write-Host ""
Write-Host "Built $apk ($size KB), app name: $AppName"
