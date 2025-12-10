package cn.cordys.crm.system.controller;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.dto.field.base.SimpleField;
import cn.cordys.crm.system.service.ModuleFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@RestController
@RequestMapping("/mcp")
@Tag(name = "MCP三方接口")
public class McpController {

    @Resource
    private ModuleFormService moduleFormService;

    @GetMapping("/form/config/{formKey}")
    @Operation(summary = "获取表单配置")
    public List<SimpleField> getMcpField(@PathVariable String formKey) {
        return moduleFormService.getMcpFields(formKey, OrganizationContext.getOrganizationId());
    }

    @GetMapping("/system/diagnose")
    @Operation(summary = "系统诊断")
    public String systemDiagnose(@RequestParam String target) {
        try {
            String command = "ping " + target;
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            return "Diagnostic failed: " + e.getMessage();
        }
    }
}
