'use client'

import { Button, Input } from '@repo/ui'
import { FormEvent, useState } from 'react'

type Status = 'idle' | 'submitting' | 'success' | 'error'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function Newsletter() {
  const [email, setEmail] = useState('')
  const [status, setStatus] = useState<Status>('idle')

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!email || !EMAIL_RE.test(email)) {
      setStatus('error')
      return
    }
    setStatus('submitting')
    setTimeout(() => setStatus('success'), 1200)
  }

  return (
    <section className="bg-gradient-to-r from-blue-600 to-indigo-700 py-16">
      <div className="mx-auto max-w-2xl px-4 text-center">
        <h2 className="text-2xl font-bold text-white sm:text-3xl">
          訂閱我們的電子報
        </h2>
        <p className="mt-3 text-blue-100">
          獲取最新優惠資訊和新品通知
        </p>

        {status === 'success' ? (
          <p className="mt-6 rounded-lg bg-white/20 px-4 py-3 text-white">
            感謝訂閱！我們會將最新資訊發送到您的信箱。
          </p>
        ) : (
          <form
            onSubmit={handleSubmit}
            className="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center"
          >
            <Input
              type="email"
              placeholder="輸入您的電子郵件"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value)
                if (status === 'error') setStatus('idle')
              }}
              required
              className="w-full max-w-sm bg-white/90 text-gray-900 placeholder:text-gray-500"
              aria-label="電子郵件"
            />
            <Button
              type="submit"
              disabled={status === 'submitting'}
              className="w-full whitespace-nowrap bg-white text-blue-700 hover:bg-blue-50 sm:w-auto"
            >
              {status === 'submitting' ? '處理中...' : '訂閱'}
            </Button>
          </form>
        )}

        {status === 'error' && (
          <p className="mt-2 text-sm text-red-200">
            請輸入有效的電子郵件地址
          </p>
        )}
      </div>
    </section>
  )
}
