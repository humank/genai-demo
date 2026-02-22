'use client'

import { useEffect } from 'react'

export default function GlobalError({
    error,
    reset,
}: {
    error: Error & { digest?: string }
    reset: () => void
}) {
    useEffect(() => {
        console.error('全局錯誤:', error)
    }, [error])

    return (
        <html lang="zh-TW">
            <body>
                <div
                    style={{
                        minHeight: '100vh',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontFamily: 'system-ui, sans-serif',
                        backgroundColor: '#fafafa',
                    }}
                >
                    <div style={{ textAlign: 'center', padding: '2rem', maxWidth: '28rem' }}>
                        <div style={{ fontSize: '4rem', marginBottom: '1rem' }}>⚠️</div>
                        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                            應用發生嚴重錯誤
                        </h1>
                        <p style={{ color: '#6b7280', marginBottom: '1.5rem' }}>
                            應用遇到了無法恢復的錯誤，請重新載入頁面。
                        </p>
                        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>
                            <button
                                onClick={reset}
                                style={{
                                    padding: '0.5rem 1.5rem',
                                    backgroundColor: '#18181b',
                                    color: '#fff',
                                    border: 'none',
                                    borderRadius: '0.375rem',
                                    cursor: 'pointer',
                                    fontSize: '0.875rem',
                                }}
                            >
                                重試
                            </button>
                            <button
                                onClick={() => (window.location.href = '/')}
                                style={{
                                    padding: '0.5rem 1.5rem',
                                    backgroundColor: 'transparent',
                                    color: '#18181b',
                                    border: '1px solid #d4d4d8',
                                    borderRadius: '0.375rem',
                                    cursor: 'pointer',
                                    fontSize: '0.875rem',
                                }}
                            >
                                返回首頁
                            </button>
                        </div>
                    </div>
                </div>
            </body>
        </html>
    )
}
