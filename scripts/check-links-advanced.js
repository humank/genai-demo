#!/usr/bin/env node

/**
 * Advanced Link Checker for Documentation
 * Checks both internal and external links with detailed reporting
 * Supports markdown files and provides comprehensive analysis
 */

const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { URL } = require('url');

// Configuration
const CONFIG = {
    timeout: 10000,
    userAgent: 'Documentation-Link-Checker/1.0',
    maxRedirects: 5,
    excludePatterns: [
        /localhost/,
        /127\.0\.0\.1/,
        /example\.com/,
        /placeholder\./
    ],
    includeExternal: process.argv.includes('--external'),
    verbose: process.argv.includes('--verbose'),
    outputFile: process.argv.includes('--output') ? 
        process.argv[process.argv.indexOf('--output') + 1] : 
        'build/reports/documentation-quality/advanced-link-check.json'
};

// Colors for console output
const colors = {
    reset: '\x1b[0m',
    red: '\x1b[31m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    purple: '\x1b[35m'
};

// Results tracking
const results = {
    totalFiles: 0,
    totalLinks: 0,
    internalLinks: 0,
    externalLinks: 0,
    validLinks: 0,
    brokenLinks: 0,
    warnings: 0,
    files: {},
    brokenLinksList: [],
    warningsList: [],
    summary: {}
};

/**
 * Log with colors
 */
function log(message, color = 'reset') {
    console.log(`${colors[color]}${message}${colors.reset}`);
}

/**
 * Extract links from markdown content
 */
function extractLinks(content, filePath) {
    const links = [];
    
    // Markdown links: [text](url)
    const markdownLinkRegex = /\[([^\]]*)\]\(([^)]+)\)/g;
    let match;
    
    while ((match = markdownLinkRegex.exec(content)) !== null) {
        const [fullMatch, text, url] = match;
        const lineNumber = content.substring(0, match.index).split('\n').length;
        
        links.push({
            type: 'markdown',
            text: text.trim(),
            url: url.trim(),
            line: lineNumber,
            raw: fullMatch
        });
    }
    
    // HTML links: <a href="url">
    const htmlLinkRegex = /<a\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*>/gi;
    
    while ((match = htmlLinkRegex.exec(content)) !== null) {
        const [fullMatch, url] = match;
        const lineNumber = content.substring(0, match.index).split('\n').length;
        
        links.push({
            type: 'html',
            text: 'HTML link',
            url: url.trim(),
            line: lineNumber,
            raw: fullMatch
        });
    }
    
    return links;
}

/**
 * Check if URL is external
 */
function isExternalUrl(url) {
    return /^https?:\/\//.test(url);
}

/**
 * Resolve relative path
 */
function resolveRelativePath(basePath, relativePath) {
    if (relativePath.startsWith('/')) {
        return path.join(process.cwd(), relativePath.substring(1));
    }
    
    const baseDir = path.dirname(basePath);
    return path.resolve(baseDir, relativePath);
}

/**
 * Check internal link
 */
function checkInternalLink(url, basePath) {
    return new Promise((resolve) => {
        try {
            // Remove anchor
            const cleanUrl = url.split('#')[0];
            if (!cleanUrl) {
                resolve({ valid: true, status: 'anchor-only' });
                return;
            }
            
            const targetPath = resolveRelativePath(basePath, cleanUrl);
            
            if (fs.existsSync(targetPath)) {
                const stats = fs.statSync(targetPath);
                resolve({ 
                    valid: true, 
                    status: stats.isDirectory() ? 'directory' : 'file',
                    targetPath 
                });
            } else {
                resolve({ 
                    valid: false, 
                    status: 'not-found',
                    targetPath,
                    error: 'File or directory not found'
                });
            }
        } catch (error) {
            resolve({ 
                valid: false, 
                status: 'error',
                error: error.message 
            });
        }
    });
}

/**
 * Check external link
 */
function checkExternalLink(url) {
    return new Promise((resolve) => {
        try {
            // Skip excluded patterns
            if (CONFIG.excludePatterns.some(pattern => pattern.test(url))) {
                resolve({ valid: true, status: 'excluded', statusCode: 'N/A' });
                return;
            }
            
            const urlObj = new URL(url);
            const isHttps = urlObj.protocol === 'https:';
            const client = isHttps ? https : http;
            
            const options = {
                hostname: urlObj.hostname,
                port: urlObj.port || (isHttps ? 443 : 80),
                path: urlObj.pathname + urlObj.search,
                method: 'HEAD',
                timeout: CONFIG.timeout,
                headers: {
                    'User-Agent': CONFIG.userAgent
                }
            };
            
            const req = client.request(options, (res) => {
                const statusCode = res.statusCode;
                
                if (statusCode >= 200 && statusCode < 300) {
                    resolve({ valid: true, status: 'ok', statusCode });
                } else if (statusCode >= 300 && statusCode < 400) {
                    resolve({ valid: true, status: 'redirect', statusCode });
                } else {
                    resolve({ valid: false, status: 'error', statusCode });
                }
            });
            
            req.on('timeout', () => {
                req.destroy();
                resolve({ valid: false, status: 'timeout', error: 'Request timeout' });
            });
            
            req.on('error', (error) => {
                resolve({ valid: false, status: 'error', error: error.message });
            });
            
            req.end();
            
        } catch (error) {
            resolve({ valid: false, status: 'error', error: error.message });
        }
    });
}

/**
 * Process a single file
 */
async function processFile(filePath) {
    try {
        const content = fs.readFileSync(filePath, 'utf8');
        const links = extractLinks(content, filePath);
        
        const fileResults = {
            path: filePath,
            totalLinks: links.length,
            internalLinks: 0,
            externalLinks: 0,
            validLinks: 0,
            brokenLinks: 0,
            warnings: 0,
            links: []
        };
        
        if (CONFIG.verbose) {
            log(`Processing: ${filePath} (${links.length} links)`, 'blue');
        }
        
        for (const link of links) {
            const linkResult = {
                ...link,
                isExternal: isExternalUrl(link.url),
                valid: false,
                status: 'unchecked',
                details: {}
            };
            
            if (linkResult.isExternal) {
                fileResults.externalLinks++;
                results.externalLinks++;
                
                if (CONFIG.includeExternal) {
                    const checkResult = await checkExternalLink(link.url);
                    linkResult.valid = checkResult.valid;
                    linkResult.status = checkResult.status;
                    linkResult.details = checkResult;
                    
                    if (CONFIG.verbose) {
                        const status = checkResult.valid ? '✅' : '❌';
                        log(`  ${status} External: ${link.url} (${checkResult.status})`, 
                            checkResult.valid ? 'green' : 'red');
                    }
                } else {
                    linkResult.valid = true;
                    linkResult.status = 'skipped';
                }
            } else {
                fileResults.internalLinks++;
                results.internalLinks++;
                
                const checkResult = await checkInternalLink(link.url, filePath);
                linkResult.valid = checkResult.valid;
                linkResult.status = checkResult.status;
                linkResult.details = checkResult;
                
                if (CONFIG.verbose) {
                    const status = checkResult.valid ? '✅' : '❌';
                    log(`  ${status} Internal: ${link.url} (${checkResult.status})`, 
                        checkResult.valid ? 'green' : 'red');
                }
            }
            
            if (linkResult.valid) {
                fileResults.validLinks++;
                results.validLinks++;
            } else {
                fileResults.brokenLinks++;
                results.brokenLinks++;
                results.brokenLinksList.push({
                    file: filePath,
                    line: link.line,
                    url: link.url,
                    error: linkResult.details.error || linkResult.status
                });
            }
            
            fileResults.links.push(linkResult);
        }
        
        results.files[filePath] = fileResults;
        results.totalLinks += fileResults.totalLinks;
        
    } catch (error) {
        log(`Error processing ${filePath}: ${error.message}`, 'red');
        results.warningsList.push({
            file: filePath,
            error: error.message
        });
    }
}

/**
 * Find all markdown files
 */
function findMarkdownFiles(dir = '.', files = []) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    
    for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        
        if (entry.isDirectory()) {
            // Skip certain directories
            if (!['node_modules', '.git', '.kiro'].includes(entry.name)) {
                findMarkdownFiles(fullPath, files);
            }
        } else if (entry.isFile() && entry.name.endsWith('.md')) {
            files.push(fullPath);
        }
    }
    
    return files;
}

/**
 * Generate report
 */
function generateReport() {
    results.summary = {
        totalFiles: results.totalFiles,
        totalLinks: results.totalLinks,
        internalLinks: results.internalLinks,
        externalLinks: results.externalLinks,
        validLinks: results.validLinks,
        brokenLinks: results.brokenLinks,
        warnings: results.warnings,
        successRate: results.totalLinks > 0 ? 
            ((results.validLinks / results.totalLinks) * 100).toFixed(2) : 0,
        timestamp: new Date().toISOString()
    };
    
    // Ensure output directory exists
    const outputDir = path.dirname(CONFIG.outputFile);
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }
    
    // Write JSON report
    fs.writeFileSync(CONFIG.outputFile, JSON.stringify(results, null, 2));
    
    // Generate markdown summary
    const markdownReport = CONFIG.outputFile.replace('.json', '.md');
    const markdown = `# Advanced Link Check Report

**Generated:** ${new Date().toISOString()}  
**Configuration:** ${CONFIG.includeExternal ? 'Including external links' : 'Internal links only'}

## Summary

- **Total Files:** ${results.totalFiles}
- **Total Links:** ${results.totalLinks}
- **Internal Links:** ${results.internalLinks}
- **External Links:** ${results.externalLinks}
- **Valid Links:** ${results.validLinks}
- **Broken Links:** ${results.brokenLinks}
- **Success Rate:** ${results.summary.successRate}%

## Broken Links

${results.brokenLinksList.length === 0 ? 'No broken links found! 🎉' : 
results.brokenLinksList.map(item => 
    `- **${item.file}:${item.line}** - ${item.url} (${item.error})`
).join('\n')}

## Warnings

${results.warningsList.length === 0 ? 'No warnings.' : 
results.warningsList.map(item => 
    `- **${item.file}** - ${item.error}`
).join('\n')}

## Files Processed

${Object.keys(results.files).map(file => {
    const fileResult = results.files[file];
    return `- **${file}** - ${fileResult.totalLinks} links (${fileResult.validLinks} valid, ${fileResult.brokenLinks} broken)`;
}).join('\n')}
`;
    
    fs.writeFileSync(markdownReport, markdown);
    
    return { jsonReport: CONFIG.outputFile, markdownReport };
}

/**
 * Main execution
 */
async function main() {
    log('🔗 Advanced Link Checker Starting...', 'purple');
    log(`Configuration: ${CONFIG.includeExternal ? 'Including external links' : 'Internal links only'}`, 'blue');
    
    const files = findMarkdownFiles();
    results.totalFiles = files.length;
    
    log(`Found ${files.length} markdown files to process`, 'blue');
    
    // Process files
    for (const file of files) {
        await processFile(file);
    }
    
    // Generate reports
    const reports = generateReport();
    
    // Console summary
    log('\n📊 Link Check Complete', 'purple');
    log('===================', 'purple');
    log(`Total Files: ${results.totalFiles}`, 'blue');
    log(`Total Links: ${results.totalLinks}`, 'blue');
    log(`Valid Links: ${results.validLinks}`, 'green');
    log(`Broken Links: ${results.brokenLinks}`, results.brokenLinks > 0 ? 'red' : 'green');
    log(`Success Rate: ${results.summary.successRate}%`, 
        results.summary.successRate >= 95 ? 'green' : 
        results.summary.successRate >= 80 ? 'yellow' : 'red');
    
    log(`\nReports generated:`, 'blue');
    log(`- JSON: ${reports.jsonReport}`, 'blue');
    log(`- Markdown: ${reports.markdownReport}`, 'blue');
    
    // Exit with appropriate code
    process.exit(results.brokenLinks > 0 ? 1 : 0);
}

// Handle command line help
if (process.argv.includes('--help') || process.argv.includes('-h')) {
    console.log(`
Advanced Link Checker for Documentation

Usage: node check-links-advanced.js [options]

Options:
  --external     Include external link checking (slower)
  --verbose      Show detailed progress
  --output FILE  Specify output file (default: build/reports/documentation-quality/advanced-link-check.json)
  --help, -h     Show this help message

Examples:
  node check-links-advanced.js
  node check-links-advanced.js --external --verbose
  node check-links-advanced.js --output reports/links.json
`);
    process.exit(0);
}

// Run main function
main().catch(error => {
    log(`Fatal error: ${error.message}`, 'red');
    process.exit(1);
});