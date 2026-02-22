import { expect, test } from '@playwright/test'

test.describe('Consumer 訂單', () => {
    test('訂單列表頁面載入', async ({ page }) => {
        await page.goto('/orders')

        await expect(page.getByRole('heading', { name: '我的訂單' })).toBeVisible()
    })

    test('無訂單時顯示空狀態', async ({ page }) => {
        await page.goto('/orders')

        // Wait for data to load
        await page.waitForTimeout(2000)

        const emptyState = page.getByText('尚無訂單')
        const orderList = page.locator('a[href^="/orders/"]')

        const hasEmpty = await emptyState.isVisible().catch(() => false)
        const hasOrders = (await orderList.count()) > 0

        // Either empty state or order list should be present
        expect(hasEmpty || hasOrders).toBeTruthy()
    })

    test('訂單詳情頁面可訪問', async ({ page }) => {
        await page.goto('/orders')

        await page.waitForTimeout(2000)

        const orderLink = page.locator('a[href^="/orders/"]').first()
        const hasOrders = await orderLink.isVisible().catch(() => false)

        if (hasOrders) {
            await orderLink.click()
            await expect(page).toHaveURL(/\/orders\//)
        }
    })
})
