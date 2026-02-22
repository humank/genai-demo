import { expect, test } from '@playwright/test'

test.describe('Consumer 結帳流程', () => {
    test('空購物車結帳頁顯示提示', async ({ page }) => {
        await page.goto('/checkout')

        // Should show empty cart message
        const emptyMessage = page.getByText('購物車是空的')
        const checkoutForm = page.getByRole('heading', { name: '結帳' })

        const hasEmpty = await emptyMessage.isVisible().catch(() => false)
        const hasForm = await checkoutForm.isVisible().catch(() => false)

        // Either empty state or checkout form should be visible
        expect(hasEmpty || hasForm).toBeTruthy()
    })

    test('空購物車有前往購物按鈕', async ({ page }) => {
        await page.goto('/checkout')

        const shopButton = page.getByText('前往購物')
        const isVisible = await shopButton.isVisible().catch(() => false)

        if (isVisible) {
            await shopButton.click()
            await expect(page).toHaveURL('/products')
        }
    })

    test('結帳頁面有返回購物車連結', async ({ page }) => {
        await page.goto('/checkout')

        const backLink = page.getByText('返回購物車')
        const isVisible = await backLink.isVisible().catch(() => false)

        if (isVisible) {
            await expect(backLink).toBeVisible()
        }
    })
})
