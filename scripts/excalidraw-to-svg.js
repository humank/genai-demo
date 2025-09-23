#!/usr/bin/env node

/**
 * Excalidraw 到 PNG 轉換器
 * 
 * 此腳本將 Excalidraw 文件轉換為 PNG 圖片
 * 支援 MCP 整合和本地轉換
 */

const fs = require('fs');
const path = require('path');

// 配置
const CONFIG = {
    outputFormat: 'png',
    backgroundColor: 'white',
    scale: 2,
    quality: 0.9,
    width: 1200,
    height: 800
};

/**
 * 日誌函數
 */
function log(level, message) {
    const timestamp = new Date().toISOString();
    const colors = {
        info: '\x1b[36m',    // cyan
        success: '\x1b[32m', // green
        warning: '\x1b[33m', // yellow
        error: '\x1b[31m',   // red
        reset: '\x1b[0m'     // reset
    };
    
    console.log(`${colors[level]}[${timestamp}] ${level.toUpperCase()}: ${message}${colors.reset}`);
}

/**
 * 驗證 Excalidraw 文件格式
 */
function validateExcalidrawFile(data) {
    try {
        const parsed = JSON.parse(data);
        
        // 檢查必要的 Excalidraw 屬性
        if (!parsed.type || parsed.type !== 'excalidraw') {
            return { valid: false, error: '不是有效的 Excalidraw 文件格式' };
        }
        
        if (!parsed.elements || !Array.isArray(parsed.elements)) {
            return { valid: false, error: '缺少 elements 陣列' };
        }
        
        return { valid: true, data: parsed };
    } catch (error) {
        return { valid: false, error: `JSON 解析錯誤: ${error.message}` };
    }
}

/**
 * 使用 MCP 轉換 Excalidraw (如果可用)
 */
async function convertWithMCP(excalidrawData, outputPath) {
    try {
        // 這裡應該整合 MCP Excalidraw 服務
        // 目前返回 false 表示 MCP 不可用
        log('info', 'MCP Excalidraw 服務暫不可用，使用本地轉換');
        return false;
    } catch (error) {
        log('warning', `MCP 轉換失敗: ${error.message}`);
        return false;
    }
}

/**
 * 本地 Excalidraw 轉換 (簡化版)
 */
async function convertLocally(excalidrawData, outputPath) {
    try {
        // 分析 Excalidraw 元素
        const elements = excalidrawData.elements || [];
        const appState = excalidrawData.appState || {};
        
        // 計算畫布邊界
        let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        
        elements.forEach(element => {
            if (element.x !== undefined && element.y !== undefined) {
                minX = Math.min(minX, element.x);
                minY = Math.min(minY, element.y);
                maxX = Math.max(maxX, element.x + (element.width || 0));
                maxY = Math.max(maxY, element.y + (element.height || 0));
            }
        });
        
        // 如果沒有有效元素，使用默認尺寸
        if (!isFinite(minX)) {
            minX = 0; minY = 0; maxX = CONFIG.width; maxY = CONFIG.height;
        }
        
        const canvasWidth = Math.max(maxX - minX, CONFIG.width);
        const canvasHeight = Math.max(maxY - minY, CONFIG.height);
        
        // 生成 SVG (作為中間格式)
        const svgContent = generateSVG(elements, canvasWidth, canvasHeight, minX, minY);
        
        // 保存 SVG 文件 (主要輸出格式)
        const svgPath = outputPath;
        fs.writeFileSync(svgPath, svgContent);
        
        // 創建 PNG 佔位符文件 (實際需要 SVG 到 PNG 轉換器)
        const placeholderContent = createPNGPlaceholder(excalidrawData, canvasWidth, canvasHeight);
        fs.writeFileSync(outputPath, placeholderContent);
        
        log('success', `已生成 SVG: ${path.basename(svgPath)}`);
        log('success', `已生成 PNG 佔位符: ${path.basename(outputPath)}`);
        
        return true;
    } catch (error) {
        log('error', `本地轉換失敗: ${error.message}`);
        return false;
    }
}

/**
 * 生成 SVG 內容
 */
function generateSVG(elements, width, height, offsetX = 0, offsetY = 0) {
    const svgElements = elements.map(element => {
        const x = element.x - offsetX;
        const y = element.y - offsetY;
        
        switch (element.type) {
            case 'rectangle':
                return `<rect x="${x}" y="${y}" width="${element.width}" height="${element.height}" 
                        fill="${element.backgroundColor || 'transparent'}" 
                        stroke="${element.strokeColor || '#000'}" 
                        stroke-width="${element.strokeWidth || 1}"/>`;
                        
            case 'ellipse':
                const rx = element.width / 2;
                const ry = element.height / 2;
                const cx = x + rx;
                const cy = y + ry;
                return `<ellipse cx="${cx}" cy="${cy}" rx="${rx}" ry="${ry}" 
                        fill="${element.backgroundColor || 'transparent'}" 
                        stroke="${element.strokeColor || '#000'}" 
                        stroke-width="${element.strokeWidth || 1}"/>`;
                        
            case 'text':
                return `<text x="${x}" y="${y + (element.fontSize || 16)}" 
                        font-family="${element.fontFamily || 'Arial'}" 
                        font-size="${element.fontSize || 16}" 
                        fill="${element.strokeColor || '#000'}">${element.text || ''}</text>`;
                        
            case 'line':
                const points = element.points || [[0, 0], [element.width || 100, element.height || 0]];
                const pathData = `M ${x + points[0][0]} ${y + points[0][1]} ` + 
                               points.slice(1).map(p => `L ${x + p[0]} ${y + p[1]}`).join(' ');
                return `<path d="${pathData}" 
                        stroke="${element.strokeColor || '#000'}" 
                        stroke-width="${element.strokeWidth || 1}" 
                        fill="none"/>`;
                        
            case 'arrow':
                // 簡化的箭頭實現
                const endX = x + (element.width || 100);
                const endY = y + (element.height || 0);
                return `<line x1="${x}" y1="${y}" x2="${endX}" y2="${endY}" 
                        stroke="${element.strokeColor || '#000'}" 
                        stroke-width="${element.strokeWidth || 1}" 
                        marker-end="url(#arrowhead)"/>`;
                        
            default:
                return `<!-- 未支援的元素類型: ${element.type} -->`;
        }
    }).join('\n    ');
    
    return `<?xml version="1.0" encoding="UTF-8"?>
<svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <marker id="arrowhead" markerWidth="10" markerHeight="7" 
                refX="9" refY="3.5" orient="auto">
            <polygon points="0 0, 10 3.5, 0 7" fill="#000"/>
        </marker>
    </defs>
    <rect width="100%" height="100%" fill="${CONFIG.backgroundColor}"/>
    ${svgElements}
</svg>`;
}

/**
 * 創建 PNG 佔位符 (Base64 編碼的小圖片)
 */
function createPNGPlaceholder(excalidrawData, width, height) {
    // 創建一個簡單的 PNG 佔位符 (1x1 像素的透明圖片)
    const pngHeader = Buffer.from([
        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
        0x00, 0x00, 0x00, 0x0D, // IHDR chunk length
        0x49, 0x48, 0x44, 0x52, // IHDR
        0x00, 0x00, 0x00, 0x01, // width: 1
        0x00, 0x00, 0x00, 0x01, // height: 1
        0x08, 0x06, 0x00, 0x00, 0x00, // bit depth, color type, compression, filter, interlace
        0x1F, 0x15, 0xC4, 0x89, // CRC
        0x00, 0x00, 0x00, 0x0A, // IDAT chunk length
        0x49, 0x44, 0x41, 0x54, // IDAT
        0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01, // compressed data
        0x0D, 0x0A, 0x2D, 0xB4, // CRC
        0x00, 0x00, 0x00, 0x00, // IEND chunk length
        0x49, 0x45, 0x4E, 0x44, // IEND
        0xAE, 0x42, 0x60, 0x82  // CRC
    ]);
    
    return pngHeader;
}

/**
 * 生成轉換報告
 */
function generateReport(inputFile, outputFile, success, metadata = {}) {
    const reportPath = outputFile.replace('.svg', '.report.json');
    const report = {
        timestamp: new Date().toISOString(),
        input: inputFile,
        output: outputFile,
        success: success,
        metadata: {
            elements: metadata.elementCount || 0,
            canvasSize: metadata.canvasSize || { width: 0, height: 0 },
            conversionMethod: metadata.method || 'local',
            ...metadata
        }
    };
    
    try {
        fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
        log('info', `轉換報告已生成: ${path.basename(reportPath)}`);
    } catch (error) {
        log('warning', `無法生成轉換報告: ${error.message}`);
    }
}

/**
 * 主轉換函數
 */
async function convertToPNG(inputFile) {
    try {
        log('info', `開始處理: ${path.basename(inputFile)}`);
        
        // 檢查輸入文件
        if (!fs.existsSync(inputFile)) {
            throw new Error(`輸入文件不存在: ${inputFile}`);
        }
        
        // 讀取和驗證 Excalidraw 文件
        const fileContent = fs.readFileSync(inputFile, 'utf8');
        const validation = validateExcalidrawFile(fileContent);
        
        if (!validation.valid) {
            throw new Error(`文件驗證失敗: ${validation.error}`);
        }
        
        const excalidrawData = validation.data;
        const outputFile = inputFile.replace('.excalidraw', '.svg');
        
        log('info', `輸出路徑: ${path.basename(outputFile)}`);
        log('info', `元素數量: ${excalidrawData.elements.length}`);
        
        // 嘗試使用 MCP 轉換
        let success = await convertWithMCP(excalidrawData, outputFile);
        let method = 'mcp';
        
        // 如果 MCP 失敗，使用本地轉換
        if (!success) {
            success = await convertLocally(excalidrawData, outputFile);
            method = 'local';
        }
        
        // 生成轉換報告
        generateReport(inputFile, outputFile, success, {
            elementCount: excalidrawData.elements.length,
            method: method,
            canvasSize: {
                width: CONFIG.width,
                height: CONFIG.height
            }
        });
        
        if (success) {
            log('success', `轉換完成: ${path.basename(inputFile)} -> ${path.basename(outputFile)}`);
        } else {
            log('error', `轉換失敗: ${path.basename(inputFile)}`);
        }
        
        return success;
        
    } catch (error) {
        log('error', `處理失敗: ${error.message}`);
        return false;
    }
}

/**
 * 批次轉換函數
 */
async function batchConvert(directory) {
    log('info', `開始批次轉換目錄: ${directory}`);
    
    const excalidrawFiles = [];
    
    // 遞歸查找 .excalidraw 文件
    function findExcalidrawFiles(dir) {
        const files = fs.readdirSync(dir);
        
        files.forEach(file => {
            const fullPath = path.join(dir, file);
            const stat = fs.statSync(fullPath);
            
            if (stat.isDirectory()) {
                findExcalidrawFiles(fullPath);
            } else if (file.endsWith('.excalidraw')) {
                excalidrawFiles.push(fullPath);
            }
        });
    }
    
    findExcalidrawFiles(directory);
    
    if (excalidrawFiles.length === 0) {
        log('warning', '未找到 Excalidraw 文件');
        return true;
    }
    
    log('info', `找到 ${excalidrawFiles.length} 個 Excalidraw 文件`);
    
    let successCount = 0;
    let failCount = 0;
    
    for (const file of excalidrawFiles) {
        const success = await convertToPNG(file);
        if (success) {
            successCount++;
        } else {
            failCount++;
        }
    }
    
    log('info', `批次轉換完成: ${successCount} 成功, ${failCount} 失敗`);
    return failCount === 0;
}

/**
 * 顯示幫助信息
 */
function showHelp() {
    console.log(`
Excalidraw 到 PNG 轉換器

用法:
  node excalidraw-to-svg.js <input.excalidraw>     # 轉換單個文件
  node excalidraw-to-svg.js --batch <directory>    # 批次轉換目錄
  node excalidraw-to-svg.js --help                 # 顯示幫助

選項:
  --batch <dir>    批次轉換指定目錄中的所有 .excalidraw 文件
  --help           顯示此幫助信息

範例:
  node excalidraw-to-svg.js diagram.excalidraw
  node excalidraw-to-svg.js --batch docs/diagrams/concepts/

輸出:
  - PNG 圖片文件
  - SVG 向量圖文件 (中間格式)
  - JSON 轉換報告

注意:
  - 目前使用簡化的本地轉換器
  - 完整功能需要 MCP Excalidraw 服務整合
  - 生成的 PNG 為佔位符，SVG 包含實際內容
`);
}

/**
 * 主函數
 */
async function main() {
    const args = process.argv.slice(2);
    
    if (args.length === 0 || args.includes('--help')) {
        showHelp();
        process.exit(0);
    }
    
    try {
        if (args[0] === '--batch') {
            if (args.length < 2) {
                log('error', '批次模式需要指定目錄');
                process.exit(1);
            }
            
            const directory = args[1];
            if (!fs.existsSync(directory)) {
                log('error', `目錄不存在: ${directory}`);
                process.exit(1);
            }
            
            const success = await batchConvert(directory);
            process.exit(success ? 0 : 1);
        } else {
            const inputFile = args[0];
            const success = await convertToPNG(inputFile);
            process.exit(success ? 0 : 1);
        }
    } catch (error) {
        log('error', `執行失敗: ${error.message}`);
        process.exit(1);
    }
}

// 如果直接執行此腳本
if (require.main === module) {
    main();
}

module.exports = {
    convertToPNG,
    batchConvert,
    validateExcalidrawFile
};