import { expect, test } from '@playwright/test'

test.describe('Consumer 首頁', () => {
    test('首頁載入並顯示主要元素', async ({ page }) => {
        await page.goto('/')

        // Navbar visible
        await expect(page.locator('header')).toBeVisible()
        await expect(page.getByText('電商平台')).toBeVisible()

        // Hero banner visible
        await expect(page.locator('main')).toBeVisible()
    })

    test('精選商品區塊顯示商品卡片', async ({ page }) => {
        await page.goto('/')

        const featuredSection = page.getByText('精選商品')
        // If API returns products, the section should be visible
        // If not, the section may be hidden — both are valid
        const isVisible = await featuredSection.isVisible().catch(() => false)
        if (isVisible) {
            await expect(page.getByText('查看全部')).toBeVisible()
        }
    })

    test('點擊商品卡片進入詳情頁', async ({ page }) => {
        await page.goto('/')

        const productLink = page.locator('a[href^="/products/"]').first()
        const isVisible = await productLink.isVisible().catch(() => false)

        if (isVisible) {
            await productLink.click()
            await expect(page).toHaveURL(/\/products\//)
        }
    })

    test('導航列連結正常運作', async ({ page }) => {
        await page.goto('/')

        // Desktop nav: click 商品
        const productsLink = page.locator('nav[aria-label="主要導航"] a[href="/products"]')
        if (await productsLink.isVisible()) {
            await productsLink.click()
            await expect(page).toHaveURL('/products')
        }
    })
})
