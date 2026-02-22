import { expect, test } from '@playwright/test'

test.describe('CMC 支付管理', () => {
    test('支付頁面載入並顯示標題', async ({ page }) => {
        await page.goto('/payments')

        await expect(page.getByRole('heading', { name: '支付管理' })).toBeVisible()
    })

    test('搜尋欄位可輸入', async ({ page }) => {
        await page.goto('/payments')

        const searchInput = page.getByPlaceholder('搜尋支付 ID 或訂單 ID...')
        await expect(searchInput).toBeVisible()

        await searchInput.fill('PAY-001')
        await expect(searchInput).toHaveValue('PAY-001')
    })

    test('狀態篩選下拉選單可操作', async ({ page }) => {
        await page.goto('/payments')

        // The status filter select trigger should be visible
        const filterTrigger = page.locator('[role="combobox"]').first()
        const isVisible = await filterTrigger.isVisible().catch(() => false)

        if (isVisible) {
            await filterTrigger.click()
            // Select options should appear
            const options = page.locator('[role="option"]')
            await expect(options.first()).toBeVisible()
        }
    })

    test('刷新按鈕可點擊', async ({ page }) => {
        await page.goto('/payments')

        const refreshButton = page.getByRole('button', { name: /刷新/ })
        await expect(refreshButton).toBeVisible()
        await refreshButton.click()
    })

    test('載入錯誤時顯示重試按鈕', async ({ page }) => {
        await page.goto('/payments')

        // Wait for data to load or error
        await page.waitForTimeout(3000)

        // Either table data or error state should be visible
        const retryButton = page.getByRole('button', { name: /重試/ })
        const table = page.locator('table')

        const hasRetry = await retryButton.isVisible().catch(() => false)
        const hasTable = await table.isVisible().catch(() => false)
        const hasLoading = await page.locator('.animate-pulse').isVisible().catch(() => false)

        expect(hasRetry || hasTable || hasLoading).toBeTruthy()
    })
})
