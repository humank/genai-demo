#!/usr/bin/env node

const { Server } = require('@modelcontextprotocol/sdk/server/index.js');
const { StdioServerTransport } = require('@modelcontextprotocol/sdk/server/stdio.js');
const { CallToolRequestSchema, ListToolsRequestSchema } = require('@modelcontextprotocol/sdk/types.js');

class ExcalidrawMCPServer {
    constructor() {
        this.server = new Server(
            {
                name: 'excalidraw-mcp-simple',
                version: '0.1.0',
            },
            {
                capabilities: {
                    tools: {},
                },
            }
        );

        this.setupToolHandlers();
    }

    setupToolHandlers() {
        this.server.setRequestHandler(ListToolsRequestSchema, async () => {
            return {
                tools: [
                    {
                        name: 'create_simple_diagram',
                        description: 'Create a simple Excalidraw diagram with basic shapes',
                        inputSchema: {
                            type: 'object',
                            properties: {
                                title: {
                                    type: 'string',
                                    description: 'Title for the diagram',
                                },
                                elements: {
                                    type: 'array',
                                    description: 'Array of elements to create',
                                    items: {
                                        type: 'object',
                                        properties: {
                                            type: { type: 'string', enum: ['rectangle', 'ellipse', 'text', 'arrow'] },
                                            x: { type: 'number' },
                                            y: { type: 'number' },
                                            width: { type: 'number' },
                                            height: { type: 'number' },
                                            text: { type: 'string' },
                                            backgroundColor: { type: 'string' },
                                            strokeColor: { type: 'string' }
                                        },
                                        required: ['type', 'x', 'y']
                                    }
                                }
                            },
                            required: ['title', 'elements'],
                        },
                    },
                    {
                        name: 'create_flowchart',
                        description: 'Create a simple flowchart with connected boxes',
                        inputSchema: {
                            type: 'object',
                            properties: {
                                title: { type: 'string', description: 'Flowchart title' },
                                steps: {
                                    type: 'array',
                                    description: 'Array of flowchart steps',
                                    items: {
                                        type: 'object',
                                        properties: {
                                            text: { type: 'string' },
                                            type: { type: 'string', enum: ['start', 'process', 'decision', 'end'] }
                                        },
                                        required: ['text', 'type']
                                    }
                                }
                            },
                            required: ['title', 'steps']
                        }
                    }
                ],
            };
        });

        this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
            const { name, arguments: args } = request.params;

            try {
                switch (name) {
                    case 'create_simple_diagram':
                        return await this.createSimpleDiagram(args);
                    case 'create_flowchart':
                        return await this.createFlowchart(args);
                    default:
                        throw new Error(`Unknown tool: ${name}`);
                }
            } catch (error) {
                return {
                    content: [
                        {
                            type: 'text',
                            text: `Error: ${error.message}`,
                        },
                    ],
                    isError: true,
                };
            }
        });
    }

    async createSimpleDiagram(args) {
        const { title, elements } = args;

        // Generate Excalidraw JSON format
        const excalidrawElements = elements.map((element, index) => ({
            id: `element-${index}`,
            type: element.type,
            x: element.x,
            y: element.y,
            width: element.width || 100,
            height: element.height || 50,
            angle: 0,
            strokeColor: element.strokeColor || '#000000',
            backgroundColor: element.backgroundColor || 'transparent',
            fillStyle: 'hachure',
            strokeWidth: 2,
            strokeStyle: 'solid',
            roughness: 1,
            opacity: 100,
            text: element.text || '',
            fontSize: 16,
            fontFamily: 1,
            textAlign: 'center',
            verticalAlign: 'middle',
            groupIds: [],
            frameId: null,
            roundness: null,
            seed: Math.floor(Math.random() * 1000000),
            versionNonce: Math.floor(Math.random() * 1000000),
            isDeleted: false,
            boundElements: null,
            updated: 1,
            link: null,
            locked: false
        }));

        const excalidrawData = {
            type: 'excalidraw',
            version: 2,
            source: 'https://excalidraw.com',
            elements: excalidrawElements,
            appState: {
                gridSize: null,
                viewBackgroundColor: '#ffffff'
            },
            files: {}
        };

        const jsonString = JSON.stringify(excalidrawData, null, 2);

        return {
            content: [
                {
                    type: 'text',
                    text: `Created diagram "${title}" with ${elements.length} elements.\n\nExcalidraw JSON:\n\`\`\`json\n${jsonString}\n\`\`\`\n\nYou can copy this JSON and paste it into Excalidraw (File > Open) to view the diagram.`,
                },
            ],
        };
    }

    async createFlowchart(args) {
        const { title, steps } = args;

        const elements = [];
        let currentY = 100;
        const boxWidth = 150;
        const boxHeight = 60;
        const spacing = 100;
        const centerX = 300;

        steps.forEach((step, index) => {
            // Create box
            const boxElement = {
                type: step.type === 'decision' ? 'diamond' : 'rectangle',
                x: centerX - boxWidth / 2,
                y: currentY,
                width: boxWidth,
                height: boxHeight,
                text: step.text,
                backgroundColor: this.getStepColor(step.type),
                strokeColor: '#000000'
            };
            elements.push(boxElement);

            // Create arrow to next step (except for last step)
            if (index < steps.length - 1) {
                const arrowElement = {
                    type: 'arrow',
                    x: centerX,
                    y: currentY + boxHeight,
                    width: 0,
                    height: spacing - 20,
                    strokeColor: '#000000'
                };
                elements.push(arrowElement);
            }

            currentY += boxHeight + spacing;
        });

        return await this.createSimpleDiagram({ title, elements });
    }

    getStepColor(type) {
        switch (type) {
            case 'start': return '#c8e6c9';
            case 'process': return '#e3f2fd';
            case 'decision': return '#fff3e0';
            case 'end': return '#ffcdd2';
            default: return '#f5f5f5';
        }
    }

    async run() {
        const transport = new StdioServerTransport();
        await this.server.connect(transport);
        console.error('Excalidraw MCP Server running on stdio');
    }
}

if (require.main === module) {
    const server = new ExcalidrawMCPServer();
    server.run().catch(console.error);
}

module.exports = ExcalidrawMCPServer;