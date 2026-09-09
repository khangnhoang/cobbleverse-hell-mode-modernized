# Repository Tooling & Environment Setup

This document establishes the machine-level tooling contract, installation procedures, and usage guidance for working in `cobbleverse-hell-mode-modernized`.

---

## 1. Required Toolchain

All agents and contributors working in this repository require the following command-line tools:

| Category | Tool | Verification Command | Description |
| :--- | :--- | :--- | :--- |
| **Version Control** | `git` | `git --version` | Source control management and review artifact generation |
| **GitHub CLI** | `gh` | `gh --version` | Repository interaction, issue tracking, and PR workflows |
| **Search & Discovery** | `rg` | `rg --version` | ripgrep for fast text, regex, and codebase search |
| **File Discovery** | `fd` | `fd --version` | Fast directory traversal and filename resolution |
| **JSON Processing** | `jq` | `jq --version` | Command-line JSON parsing, inspection, and transformation |
| **Archive Operations** | `7z` | `7z` | High-performance archive inspection and extraction |
| **Java Platform** | `java` | `java -version` | Java 21 runtime environment (JDK 21 required) |
| **Java Compiler** | `javac` | `javac -version` | Java 21 source code compiler |
| **Java Archive** | `jar` | `jar --version` | JAR archive creation, inspection, and manifest reading |
| **Bytecode Disassembler** | `javap` | `javap -version` | Bytecode inspection and class file signature verification |
| **Dependency Analysis** | `jdeps` | `jdeps --version` | Java package and class-level dependency analysis |

---

## 2. Reverse Engineering & Decompilation

### Capability Contract
The repository requires a command-line Java decompiler capable of reconstructing Java source from compiled bytecode (`.class` or `.jar` files).

- **Contract:** Having a repository-supported Java decompiler CLI available in the host environment.
- **Current Supported & Default Tool:** `CFR 0.152`
- **Optional Alternative:** `Vineflower` (supported alternative; not required if CFR is present. Do not install both unless specifically needed).

### Machine Setup (CFR 0.152)
On Windows host systems, CFR is configured as a machine-level utility outside the repository root:

- **CFR JAR Path:**
  `%USERPROFILE%\.tools\cfr\cfr-0.152.jar`
- **CLI Shims (on PATH):**
  - Command Prompt: `%USERPROFILE%\.local\bin\cfr.cmd`
  - PowerShell: `%USERPROFILE%\.local\bin\cfr.ps1`
- **Usage:**
  ```powershell
  cfr <path/to/Target.class>
  cfr <path/to/archive.jar> --outputdir <path/to/output>
  ```

---

## 3. Tool Selection Guidance

When executing tasks in this repository, always select the most direct dedicated tool for the task:

| Task Domain | Primary Tool | Anti-Patterns to Avoid |
| :--- | :--- | :--- |
| **Text & Code Search** | `rg` | Do not use ad-hoc Python regex scripts or standard `findstr` |
| **File & Directory Discovery** | `fd` | Do not write Python `os.walk` crawlers |
| **JSON Inspection & Transform** | `jq` | Do not write ad-hoc Python `json.load()` dump scripts |
| **Archive Inspection / Extraction** | `7z` or `jar` | Do not write Python `zipfile` extraction scripts |
| **Version Control & GitHub** | `git`, `gh` | Do not manage git state through ad-hoc scripts |
| **Java Bytecode & API Inspection** | `javap`, `jdeps`, `jar` | Do not parse classfiles or byte arrays with custom scripts |
| **Source Code Reconstruction** | `cfr` | Do not attempt manual bytecode scraping |
| **Task Automation & Analysis** | `python` | Permitted **only** when a task genuinely requires multi-source data aggregation, custom domain algorithms, or canonical repo workflows (e.g., `scripts/ci/validate_repo.py`) |

---

## 4. Installation & Environment Verification

### Windows Installation Commands (WinGet)

Install the core CLI suite via Windows Package Manager:

```powershell
# Core CLI Utilities (User-scope portable packages)
winget install --id BurntSushi.ripgrep.MSVC --exact --accept-source-agreements --accept-package-agreements
winget install --id sharkdp.fd --exact --accept-source-agreements --accept-package-agreements
winget install --id jqlang.jq --exact --accept-source-agreements --accept-package-agreements

# Archive Tool (Requires adding "C:\Program Files\7-Zip" to User PATH)
winget install --id 7zip.7zip --exact --accept-source-agreements --accept-package-agreements
```

### JDK 21 Requirement
The repository requires a standard **Java Development Kit (JDK) 21**. Any compliant OpenJDK distribution (Eclipse Adoptium Temurin, Microsoft Build of OpenJDK, Amazon Corretto, Oracle JDK) is supported.

Ensure that the JDK `bin/` directory is present in the system `PATH` and `JAVA_HOME` points to the JDK root.

### CFR 0.152 Setup (User-level)
```powershell
# Create user tools directory and download CFR 0.152
New-Item -ItemType Directory -Path "$env:USERPROFILE\.tools\cfr" -Force | Out-Null
Invoke-WebRequest -Uri "https://github.com/leibnitz27/cfr/releases/download/0.152/cfr-0.152.jar" -OutFile "$env:USERPROFILE\.tools\cfr\cfr-0.152.jar"

# Create shims in a directory present in PATH (e.g., %USERPROFILE%\.local\bin)
@"
@echo off
java -jar "%USERPROFILE%\.tools\cfr\cfr-0.152.jar" %*
"@ | Set-Content -Path "$env:USERPROFILE\.local\bin\cfr.cmd" -Encoding ASCII

@"
& java -jar "`$env:USERPROFILE\.tools\cfr\cfr-0.152.jar" @args
"@ | Set-Content -Path "$env:USERPROFILE\.local\bin\cfr.ps1" -Encoding ASCII
```

### Verification Commands
Run the following verification suite to confirm the host environment is fully configured:

```powershell
git --version
gh --version
rg --version
fd --version
jq --version
7z
java -version
javac -version
jar --version
javap -version
jdeps --version
cfr --version
```

---

## 5. Scope Boundaries & Repository Invariants

1. **No In-Repo Binaries:** Tooling binaries (`.exe`, `.jar`, `.dll`) and packages belong to the host machine environment and must never be committed into the repository.
2. **Contract & Wrapper Ownership:** The repository maintains only behavioral contracts (`AGENTS.md`), documentation (`docs/tooling.md`), and lightweight automation wrappers when explicitly approved.
3. **Report Tooling Gaps:** If a required tool is missing in an execution environment, agents must report the gap rather than quietly building ad-hoc Python substitutes.
