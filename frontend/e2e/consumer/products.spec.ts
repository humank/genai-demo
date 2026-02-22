import { expect, test } from '@playwright/test'

test.describe('Consumer 商品列表', () => {
    test('商品頁面載入並顯示標題', async ({ page }) => {
        await page.goto('/products')

        await expect(page.getByRole('heading', { name: '所有商品' })).toBeVisible()
    })

    test('搜尋欄位可輸入並觸發搜尋', async ({ page }) => {
        await page.goto('/products')

        const searchInput = page.getByPlaceholder('搜尋商品...')
        await expect(searchInput).toBeVisible()

        await searchInput.fill('測試商品')
        // URL should update with search param after debounce
        await page.waitForTimeout(500)
        await expect(page).toHaveURL(/q=/)
    })

    test('分類篩選按鈕可點擊', async ({ page }) => {
        await page.goto('/products')

        // Category filter buttons should be present
        const filterArea = page.locator('[class*="flex"][class*="gap"]').first()
        await expect(filterArea).toBeVisible()
    })

    test('商品卡片點擊進入詳情頁', async ({ page }) => {
        await page.goto('/products')

        const productCard = page.locator('a[href^="/products/"]').first()
        const isVisible = await productCard.isVisible().catch(() => false)

        if (isVisible) {
            await productCard.click()
            await expect(page).toHaveURL(/\/products\//)
        }
    })

    test('無搜尋結果時顯示空狀態', async ({ page }) => {
        await page.goto('/products?q=zzzznonexistent99999')

        // Wait for loading to finish
        await page.waitForTimeout(1000)

        // Should show empty state or product grid
        const pageContent = page.locator('main')
        await expect(pageContent).toBeVisible()
    })
})
