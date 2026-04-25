# Custom API Quick Reference

## 🚀 Quick Start (5 Steps)

1. **Open Settings** → Choose tab (LLM/Image Gen/Image Edit)
2. **Click "Configure Custom APIs"** button
3. **Add Provider** → Name, URL, API Key
4. **Add Endpoint** → Choose template or configure
5. **Add Model** → Model ID, Display Name

## 📋 Common Configurations

### OpenAI Compatible

```
Provider: OpenAI
URL: https://api.openai.com
Endpoint: /v1/chat/completions
Model: gpt-4
```

### Together AI

```
Provider: Together AI
URL: https://api.together.xyz
Endpoint: /v1/chat/completions
Model: meta-llama/Llama-3-70b-chat-hf
```

### Anthropic Claude

```
Provider: Anthropic
URL: https://api.anthropic.com
Endpoint: /v1/messages
Model: claude-3-opus-20240229
```

### Replicate (Image)

```
Provider: Replicate
URL: https://api.replicate.com
Endpoint: /v1/predictions
Model: stability-ai/sdxl
```

## 🔧 Request Schema Template

```json
{
  "headers": {
    "Authorization": "Bearer {{apiKey}}",
    "Content-Type": "application/json"
  },
  "body": {
    "model": "{{model}}",
    "messages": "{{messages}}",
    "temperature": "{{temperature}}",
    "max_tokens": "{{maxTokens}}"
  }
}
```

## 📥 Response Schema Template

```json
{
  "dataPath": "choices[0].message.content",
  "errorPath": "error.message",
  "statusPath": "status",
  "imageUrlPath": "data[0].url"
}
```

## 🎯 Placeholders

| Placeholder | Description | Example |
|------------|-------------|---------|
| `{{apiKey}}` | Your API key | Bearer sk-... |
| `{{model}}` | Model ID | gpt-4 |
| `{{messages}}` | Chat messages | [{role, content}] |
| `{{prompt}}` | Text prompt | "A sunset" |
| `{{temperature}}` | Randomness | 0.7 |
| `{{maxTokens}}` | Max length | 2048 |
| `{{size}}` | Image size | 1024x1024 |

## 🛠️ Path Notation

| Format | Example | Result |
|--------|---------|--------|
| Object field | `data.content` | data → content |
| Array index | `choices[0]` | First choice |
| Nested | `choices[0].message.content` | Deep path |
| Multiple | `data[0].url` | First URL |

## ✅ Testing Checklist

- [ ] Provider added with correct URL
- [ ] API key is valid
- [ ] Endpoint path is correct
- [ ] Request schema has valid JSON
- [ ] Response schema paths are correct
- [ ] Model ID matches API
- [ ] Test connection succeeds
- [ ] Provider is enabled
- [ ] Appears in dropdown
- [ ] Makes successful API call

## ❌ Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| Connection Failed | Wrong URL | Check base URL, no trailing / |
| 401 Unauthorized | Invalid key | Verify API key |
| 404 Not Found | Wrong endpoint | Check endpoint path |
| Invalid JSON | Schema error | Validate JSON syntax |
| Empty Response | Wrong path | Check response schema paths |
| Model Not Found | Wrong model ID | Verify model exists |

## 💡 Tips

1. **Start with Templates** - Use pre-configured templates
2. **Test Incrementally** - Test after each step
3. **Check Logs** - Review app logs for details
4. **Use Postman** - Test endpoint externally first
5. **Document Setup** - Keep notes on configurations
6. **Secure Keys** - Never share API keys
7. **Monitor Usage** - Track API usage and costs
8. **Update Regularly** - Keep configurations current

## 🔐 Security

- ✅ Keys stored encrypted
- ✅ No plaintext storage
- ✅ Secure transmission
- ❌ Never share keys
- ❌ Don't commit keys to git
- ❌ Don't log keys

## 📞 Support

1. Check in-app help screen
2. Review `CUSTOM_API_USER_GUIDE.md`
3. Test with external tools (Postman)
4. Verify API documentation
5. Check app logs for errors

## 🎓 Learning Resources

- **OpenAI API Docs**: https://platform.openai.com/docs
- **Anthropic Docs**: https://docs.anthropic.com
- **Together AI Docs**: https://docs.together.ai
- **Replicate Docs**: https://replicate.com/docs
- **JSON Path Guide**: https://jsonpath.com

## 📊 Feature Comparison

| Feature | Old System | New System |
|---------|-----------|------------|
| Multiple Providers | ❌ | ✅ |
| Templates | ❌ | ✅ |
| Schema Config | ❌ | ✅ |
| Test Connection | ❌ | ✅ |
| Model Management | Limited | Full |
| Error Messages | Basic | Detailed |
| Documentation | Minimal | Comprehensive |

## 🚦 Status Indicators

- 🟢 **Enabled** - Provider is active
- 🔴 **Disabled** - Provider is inactive
- ✅ **Connected** - Test successful
- ❌ **Failed** - Test failed
- ⚠️ **Warning** - Configuration issue

## 📈 Best Practices

1. **Name Clearly** - Use descriptive provider names
2. **Test First** - Always test before using
3. **Document** - Keep configuration notes
4. **Version Control** - Track configuration changes
5. **Monitor** - Watch API usage and costs
6. **Backup** - Export configurations regularly
7. **Update** - Keep models and endpoints current
8. **Secure** - Protect API keys

## 🔄 Migration from Old System

1. Note current settings
2. Open "Configure Custom APIs"
3. Add provider with same details
4. Use OpenAI template for endpoint
5. Add models manually
6. Test connection
7. Enable provider
8. Verify in dropdown
9. Test actual API call
10. Remove old settings (optional)

## 📝 Example Workflow

```
1. Settings → LLM Configuration
2. Click "Configure Custom Text Generation APIs"
3. Click "+" to add provider
   - Name: "My OpenAI"
   - URL: "https://api.openai.com"
   - Key: "sk-..."
4. Click provider to expand
5. Click "Add Endpoint"
   - Select "OpenAI Compatible" template
   - Click "Save"
6. Click "Add Model"
   - Model ID: "gpt-4"
   - Display Name: "GPT-4"
   - Click "Add"
7. Click "Test Connection"
8. Enable provider with toggle
9. Go back to LLM Configuration
10. Select "My OpenAI" from dropdown
11. Select "gpt-4" model
12. Start chatting!
```

## 🎯 Success Criteria

- ✅ Provider appears in dropdown
- ✅ Models are selectable
- ✅ API calls succeed
- ✅ Responses are parsed correctly
- ✅ Error messages are clear
- ✅ Configuration persists
- ✅ Multiple providers work
- ✅ Switching providers works

---

**Last Updated**: 2024
**Version**: 1.0
**Status**: Production Ready
