#!/usr/bin/env bash
set -euo pipefail

# scripts/run_unit_tests.sh - Runs Helium Android Java unit tests using JUnit

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Running Helium Android Unit Tests ===${NC}"

python3 -c "
import subprocess, os, tempfile, sys, urllib.request

junit_jar = '/home/nandha/.gradle/wrapper/dists/gradle-8.14.3-all/10utluxaxniiv4wxiphsi49nj/gradle-8.14.3/lib/junit-4.13.2.jar'
hamcrest_jar = '/home/nandha/.gradle/wrapper/dists/gradle-8.14.3-all/10utluxaxniiv4wxiphsi49nj/gradle-8.14.3/lib/hamcrest-core-1.3.jar'

if not os.path.exists(junit_jar) or not os.path.exists(hamcrest_jar):
    # Fallback for CI / clean environments
    cache_dir = os.path.expanduser('~/.cache/helium_test_lib')
    os.makedirs(cache_dir, exist_ok=True)
    junit_jar = os.path.join(cache_dir, 'junit-4.13.2.jar')
    hamcrest_jar = os.path.join(cache_dir, 'hamcrest-core-1.3.jar')
    if not os.path.exists(junit_jar):
        urllib.request.urlretrieve('https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar', junit_jar)
    if not os.path.exists(hamcrest_jar):
        urllib.request.urlretrieve('https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar', hamcrest_jar)

with tempfile.TemporaryDirectory() as tmpdir:
    # Stubs
    os.makedirs(os.path.join(tmpdir, 'androidx/annotation'))
    with open(os.path.join(tmpdir, 'androidx/annotation/NonNull.java'), 'w') as f:
        f.write('package androidx.annotation; import java.lang.annotation.*; @Retention(RetentionPolicy.CLASS) @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD}) public @interface NonNull {}')
    with open(os.path.join(tmpdir, 'androidx/annotation/Nullable.java'), 'w') as f:
        f.write('package androidx.annotation; import java.lang.annotation.*; @Retention(RetentionPolicy.CLASS) @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD}) public @interface Nullable {}')

    os.makedirs(os.path.join(tmpdir, 'android/content'))
    with open(os.path.join(tmpdir, 'android/content/SharedPreferences.java'), 'w') as f:
        f.write('package android.content; public interface SharedPreferences { interface Editor { Editor putBoolean(String k, boolean v); Editor putString(String k, String v); void apply(); } boolean getBoolean(String k, boolean def); String getString(String k, String def); Editor edit(); }')
    with open(os.path.join(tmpdir, 'android/content/Context.java'), 'w') as f:
        f.write('package android.content; public abstract class Context { public static final int MODE_PRIVATE = 0; public abstract Context getApplicationContext(); public abstract SharedPreferences getSharedPreferences(String n, int m); }')

    # Compile Sources
    subprocess.run([
        'javac', '-d', tmpdir,
        os.path.join(tmpdir, 'androidx/annotation/NonNull.java'),
        os.path.join(tmpdir, 'androidx/annotation/Nullable.java'),
        os.path.join(tmpdir, 'android/content/SharedPreferences.java'),
        os.path.join(tmpdir, 'android/content/Context.java'),
        'helium-android/src/main/java/net/imput/helium/preferences/HeliumPreferences.java',
        'helium-android/src/main/java/net/imput/helium/extensions/ExtensionItem.java'
    ], check=True)

    # Compile Tests
    subprocess.run([
        'javac', '-cp', f'{tmpdir}:{junit_jar}:{hamcrest_jar}', '-d', tmpdir,
        'helium-android/src/test/java/net/imput/helium/HeliumPreferencesTest.java',
        'helium-android/src/test/java/net/imput/helium/VaultClientTest.java',
        'helium-android/src/test/java/net/imput/helium/ExtensionManagerTest.java'
    ], check=True)

    # Run Tests
    res = subprocess.run([
        'java', '-cp', f'{tmpdir}:{junit_jar}:{hamcrest_jar}',
        'org.junit.runner.JUnitCore',
        'net.imput.helium.HeliumPreferencesTest',
        'net.imput.helium.VaultClientTest',
        'net.imput.helium.ExtensionManagerTest'
    ], capture_output=True, text=True)

    print(res.stdout)
    if res.returncode != 0:
        print(res.stderr, file=sys.stderr)
        sys.exit(res.returncode)
"

echo -e "${GREEN}All unit tests passed successfully!${NC}"
