Set-Location "@ORIGINAL_LOCATION@"
trap { $host.SetShouldExit(1) }
Invoke-Expression "& `"@SCRIPT@`" $args"