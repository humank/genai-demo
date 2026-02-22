import { OrderDetail } from '@/components/order/OrderDetail'

interface Props {
    params: Promise<{ orderId: string }>
}

export default async function OrderDetailPage({ params }: Props) {
    const { orderId } = await params
    return <OrderDetail orderId={orderId} />
}
