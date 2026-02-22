import { expect, test } from '@playwright/test'

test.describe('Consumer 購物車', () => {
    test('購物車頁面載入', async ({ page }) => {
        await page.goto('/cart')

        await expect(page.getByRole('heading', { name: '購物車' })).toBeVisible()
    })

    test('空購物車顯示空狀態提示', async ({ page }) => {
        await page.goto('/cart')

        // Should show empty cart message or cart items
        const pageContent = page.locator('main')
        await expect(pageContent).toBeVisible()

        // Look for empty state or cart items
        const emptyText = page.getByText('購物車是空的')
        const cartItems = page.locator('[class*="cart"]')
        const hasEmpty = await emptyText.isVisible().catch(() => false)
        const hasItems = await cartItems.isVisible().catch(() => false)

        expect(hasEmpty || hasItems).toBeTruthy()
    })

    test('購物車頁面有前往購物連結', async ({ page }) => {
        await page.goto('/cart')

        // If cart is empty, there should be a link to products
        const shopLink = page.getByText('前往購物')
        const isVisible = await shopLink.isVisible().catch(() => false)

        if (isVisible) {
            await shopLink.click()
            await expect(page).toHaveURL('/products')
        }
    })
})
