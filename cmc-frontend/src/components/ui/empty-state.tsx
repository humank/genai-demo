'use client'

import React from 'react'
import { Button } from '@/components/ui/button'
import { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description: string
  action?: {
    label: string
    onClick: () => void
  }
  className?: string
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon,
  title,
  description,
  action,
  className
}) => {
  return (
    <div className={cn("text-center py-12 px-4", className)}>
      <div className="mx-auto w-24 h-24 bg-muted/30 rounded-full flex items-center justify-center mb-6">
        <Icon className="h-12 w-12 text-muted-foreground/50" />
      </div>
      
      <h3 className="text-lg font-semibold text-foreground mb-2">
        {title}
      </h3>
      
      <p className="text-muted-foreground mb-6 max-w-md mx-auto">
        {description}
      </p>
      
      {action && (
        <Button onClick={action.onClick} className="btn-primary">
          {action.label}
        </Button>
      )}
    </div>
  )
}
