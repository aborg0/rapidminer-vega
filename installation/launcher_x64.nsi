; Java Launcher
;--------------
 
;You want to change the next four lines
Name RapidMiner
Caption "RapidMiner Launcher"
Icon "rapidminer_icon.ico"
OutFile "../release/files/RapidMiner.exe"

# Request execution level
RequestExecutionLevel user

SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow
 
Section ""
    

System::Alloc 64
Pop $0
System::Call "*$0(i 64)"
System::Call "Kernel32::GlobalMemoryStatusEx(i r0)"
System::Call "*$0(i, i, l, l.r1, l, l, l, l, l)"
System::Free $0

System::Int64Op $1 / 1024
Pop $1
System::Int64Op $1 / 1024
Pop $1
System::Int64Op $1 * 90
Pop $1
System::Int64Op $1 / 100
Pop $1
  

; for Xmx and Xms

IntCmp $1 64 less64 less64 more64
less64: 
StrCpy $1 64
Goto after_mem_more
more64:
Goto after_mem_more


after_mem_more:

  Call PerformUpdate
    
  Call GetJRE
  Pop $R0
  
  Call GetParameters
  Pop $R1
  
  ; invoking RapidMiner via rapidminer.jar
  StrCpy $0 '"$R0" -Xmx$1m -Xms$1m -classpath "${CLASSPATH}" -Drapidminer.home=. -Drapidminer.operators.additional="${RAPIDMINER_OPERATORS_ADDITIONAL}" -jar lib/launcher.jar $R1'
  
  SetOutPath $EXEDIR
 Relaunch:
  ExecWait $0 $1
  IntCmp $1 2 Relaunch
SectionEnd

Function PerformUpdate
;
;  Check for Directory RUinstall
;  If found, copy everything from this directory and remove it 
 
  Push $R0
 
  ClearErrors
  StrCpy $R0 "$EXEDIR\RUinstall\*"
  IfFileExists $R0 UpdateFound
  StrCpy $R0 ""
        
  UpdateFound:
    CopyFiles /SILENT $EXEDIR\RUinstall\* $EXEDIR
    RmDir /r $EXEDIR\RUinstall
     
FunctionEnd

Function GetJRE
;
;  Find JRE (javaw.exe)
;  1 - in .\jre directory (JRE Installed with application)
;  2 - in JAVA_HOME environment variable
;  3 - in the registry
;  4 - assume javaw.exe in current dir or PATH
 
  Push $R0
  Push $R1
 
  ClearErrors
  StrCpy $R0 "$EXEDIR\jre\bin\javaw.exe"
  IfFileExists $R0 JreFound
  StrCpy $R0 ""
 
  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\javaw.exe"
  IfErrors 0 JreFound
 
  ClearErrors
  ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
  StrCpy $R0 "$R0\bin\javaw.exe"
 
  IfErrors 0 JreFound
  StrCpy $R0 "javaw.exe"
        
 JreFound:
  Pop $R1
  Exch $R0
FunctionEnd


 ; GetParameters
 ; input, none
 ; output, top of stack (replaces, with e.g. whatever)
 ; modifies no other variables.
Function GetParameters
 
  Push $R0
  Push $R1
  Push $R2
  Push $R3
 
  StrCpy $R2 1
  StrLen $R3 $CMDLINE
 
  ;Check for quote or space
  StrCpy $R0 $CMDLINE $R2
  StrCmp $R0 '"' 0 +3
    StrCpy $R1 '"'
    Goto loop
  StrCpy $R1 " "
 
  loop:
    IntOp $R2 $R2 + 1
    StrCpy $R0 $CMDLINE 1 $R2
    StrCmp $R0 $R1 get
    StrCmp $R2 $R3 get
    Goto loop
 
  get:
    IntOp $R2 $R2 + 1
    StrCpy $R0 $CMDLINE 1 $R2
    StrCmp $R0 " " get
    StrCpy $R0 $CMDLINE "" $R2
 
  Pop $R3
  Pop $R2
  Pop $R1
  Exch $R0
 
FunctionEnd