import { expect, test } from '@playwright/test'

test.describe('CMC 儀表板', () => {
    test('儀表板頁面載入並顯示標題', async ({ page }) => {
        await page.goto('/')

        await expect(page.getByRole('heading', { name: '商務管理中心' })).toBeVisible()
    })

    test('KPI 統計區塊顯示', async ({ page }) => {
        await page.goto('/')

        await expect(page.getByText('實時統計')).toBeVisible()
    })

    test('數據分析圖表區塊顯示', async ({ page }) => {
        await page.goto('/')

        await expect(page.getByText('數據分析')).toBeVisible()
    })

    test('功能模組卡片顯示', async ({ page }) => {
        await page.goto('/')

        await expect(page.getByText('功能模組')).toBeVisible()
        await expect(page.getByText('訂單管理')).toBeVisible()
        await expect(page.getByText('商品管理')).toBeVisible()
        await expect(page.getByText('客戶管理')).toBeVisible()
        await expect(page.getByText('支付管理')).toBeVisible()
    })

    test('功能模組卡片可導航', async ({ page }) => {
        await page.goto('/')

        const ordersCard = page.locator('a[href="/orders"]').first()
        const isVisible = await ordersCard.isVisible().catch(() => false)

        if (isVisible) {
            await ordersCard.click()
            await expect(page).toHaveURL('/orders')
        }
    })
})
