Set-Location "@ORIGINAL_LOCATION@"
try {
  Invoke-Expression "& `"@SCRIPT@`" $args"
} catch {
  Write-Error -Exception $_.Exception
  Exit 1
}
