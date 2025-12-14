# Time Capabilities Comparison

> **Last Updated**: 2025-01-22

## Overview

This document compares different methods for getting time information in Kiro.

---

## 🕐 Available Time Methods

### Method 1: System Commands (Built-in)

**How it works**: Execute bash commands directly

```bash
# Current time
date

# Formatted time
date +"%Y-%m-%d %H:%M:%S %Z"

# UTC time
date -u

# Specific timezone
TZ="America/New_York" date
```

**Capabilities**:

- ✅ Get current system time
- ✅ Format time in various ways
- ✅ Get UTC time
- ✅ Convert to specific timezones
- ✅ No additional dependencies
- ✅ Always available

**Limitations**:

- ❌ Requires bash command execution
- ❌ Manual timezone conversion logic
- ❌ Less convenient for complex operations
- ❌ Platform-dependent syntax (macOS vs Linux)

---

### Method 2: Time MCP Server (Optional)

**How it works**: Dedicated MCP server with time tools

**Configuration**:

```json
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": ["mcp-server-time"],
      "autoApprove": [
        "get_current_time",
        "get_timezone",
        "convert_time",
        "format_time",
        "calculate_time_difference"
      ]
    }
  }
}
```

**Capabilities**:

- ✅ `get_current_time` - Get current time in any timezone
- ✅ `get_timezone` - Get timezone information
- ✅ `convert_time` - Convert between timezones
- ✅ `format_time` - Format time in various formats
- ✅ `calculate_time_difference` - Calculate time differences
- ✅ Structured API with clear parameters
- ✅ Cross-platform consistency

**Limitations**:

- ❌ Requires MCP server installation
- ❌ Additional dependency to manage
- ❌ Slightly more overhead than direct commands

---

## 📊 Feature Comparison

| Feature | System Commands | Time MCP Server |
|---------|----------------|-----------------|
| **Get current time** | ✅ `date` | ✅ `get_current_time` |
| **Format time** | ✅ `date +format` | ✅ `format_time` |
| **Timezone conversion** | ⚠️ Manual | ✅ `convert_time` |
| **Time difference** | ⚠️ Manual calculation | ✅ `calculate_time_difference` |
| **Multiple timezones** | ⚠️ Multiple commands | ✅ Single call |
| **Cross-platform** | ⚠️ Syntax varies | ✅ Consistent |
| **Dependencies** | ✅ None | ❌ Requires uvx |
| **Performance** | ✅ Fast | ✅ Fast |
| **Ease of use** | ⚠️ Moderate | ✅ Easy |

---

## 🎯 Use Case Recommendations

### Use System Commands When

- ✅ Simple time queries (current time, date)
- ✅ One-off time operations
- ✅ Minimal dependencies preferred
- ✅ Quick scripts and automation

**Example**:

```bash
# Get current date for documentation
CURRENT_DATE=$(date +%Y-%m-%d)
echo "Last Updated: $CURRENT_DATE"
```

### Use Time MCP Server When

- ✅ Complex timezone conversions
- ✅ Multiple time operations in sequence
- ✅ Need structured time data
- ✅ Cross-platform consistency required
- ✅ Frequent time-related queries

**Example**:

```text
User: "What time is it in Tokyo, New York, and London?"
Kiro: Uses time MCP server to get all three times in one operation
```

---

## 💡 Practical Examples

### Example 1: Documentation Timestamps

**Using System Commands** (Recommended):

```bash
# Simple and direct
date +%Y-%m-%d
# Output: 2025-01-22
```

**Using Time MCP Server**:

```text
get_current_time(timezone="Asia/Taipei", format="YYYY-MM-DD")
```

**Winner**: System commands (simpler for this use case)

---

### Example 2: Multi-Timezone Meeting Scheduler

**Using System Commands**:

```bash
# Requires multiple commands
TZ="America/New_York" date
TZ="Europe/London" date  
TZ="Asia/Tokyo" date
```

**Using Time MCP Server**:

```text
get_current_time(timezone="America/New_York")
get_current_time(timezone="Europe/London")
get_current_time(timezone="Asia/Tokyo")
```

**Winner**: Time MCP Server (cleaner API, consistent format)

---

### Example 3: Time Difference Calculation

**Using System Commands**:

```bash
# Complex manual calculation needed
start_time=$(date +%s)
# ... do something ...
end_time=$(date +%s)
diff=$((end_time - start_time))
echo "$diff seconds"
```

**Using Time MCP Server**:

```text
calculate_time_difference(
  start_time="2025-01-22T10:00:00Z",
  end_time="2025-01-22T14:30:00Z"
)
# Returns: 4 hours 30 minutes
```

**Winner**: Time MCP Server (much simpler)

---

## 🔧 Current Configuration Analysis

### Your Current Setup

**Project Config**: Time MCP server is **enabled**

```json
"time": {
  "command": "uvx",
  "args": ["mcp-server-time"],
  "disabled": false
}
```

**Global Config**: Time MCP server is **disabled**

```json
"time": {
  "command": "uvx",
  "args": ["mcp-server-time"],
  "disabled": true
}
```

### Recommendation

**Keep the Time MCP Server in Project Config** ✅

**Reasons**:

1. **Provides advanced capabilities** beyond simple date commands
2. **Already installed and working** - no reason to remove
3. **Useful for documentation** with multiple timezone support
4. **Small overhead** - minimal resource usage
5. **Better UX** - structured API vs manual bash commands

**Remove from Global Config** ✅

- It's disabled anyway
- Project config takes precedence
- Reduces configuration duplication

---

## 📋 Recommended Action

### Keep This Configuration

**Project Config** (`.kiro/settings/mcp.json`):

```json
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": ["mcp-server-time"],
      "env": {},
      "disabled": false,
      "autoApprove": [
        "get_current_time",
        "get_timezone",
        "convert_time",
        "format_time",
        "calculate_time_difference"
      ]
    }
  }
}
```

**Global Config** (`~/.kiro/settings/mcp.json`):

```json
{
  "mcpServers": {
    // Remove "time" entry completely
  }
}
```

---

## 🧪 Testing Both Methods

### Test System Commands

```bash
# Current time
date

# Formatted
date +"%Y-%m-%d %H:%M:%S"

# UTC
date -u

# Specific timezone
TZ="America/New_York" date
```

### Test Time MCP Server

```text
Ask Kiro:

- "What time is it?"
- "What time is it in Tokyo?"
- "Convert 2pm EST to Tokyo time"
- "What's the time difference between New York and London?"

```

---

## 📊 Summary

| Aspect | Recommendation |
|--------|----------------|
| **Simple time queries** | Use system commands |
| **Complex time operations** | Use Time MCP server |
| **Documentation timestamps** | Use system commands |
| **Multi-timezone support** | Use Time MCP server |
| **Project config** | ✅ Keep Time MCP server |
| **Global config** | ❌ Remove Time MCP server |

**Bottom Line**: Keep the Time MCP server in your project config. It provides valuable functionality beyond what system commands offer, and there's no downside to having it available.

---

**Related Documentation**:

- [MCP Server Analysis](./mcp-server-analysis.md)
- [MCP Cleanup Recommendations](./mcp-cleanup-recommendations.md)
