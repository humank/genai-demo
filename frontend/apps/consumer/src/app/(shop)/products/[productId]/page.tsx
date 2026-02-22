import { ProductDetail } from '@/components/product/ProductDetail'

interface Props {
    params: Promise<{ productId: string }>
}

export default async function ProductDetailPage({ params }: Props) {
    const { productId } = await params
    return <ProductDetail productId={productId} />
}
