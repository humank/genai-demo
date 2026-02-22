#!/usr/bin/env python3
"""
Convert CSV files to SQLite database for better performance and smaller size.
"""

import sqlite3
import csv
import os
from pathlib import Path

def create_database(db_path):
    """Create SQLite database with all tables."""
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # Products table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS products (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_type TEXT,
            pattern TEXT,
            description TEXT,
            priority INTEGER
        )
    ''')
    
    # Styles table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS styles (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            style_name TEXT,
            description TEXT,
            colors TEXT,
            effects TEXT,
            use_cases TEXT,
            priority INTEGER
        )
    ''')
    
    # Typography table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS typography (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            pairing_name TEXT,
            heading_font TEXT,
            body_font TEXT,
            use_case TEXT,
            priority INTEGER
        )
    ''')
    
    # Colors table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS colors (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_type TEXT,
            palette_name TEXT,
            primary_color TEXT,
            secondary_color TEXT,
            accent_color TEXT,
            priority INTEGER
        )
    ''')
    
    # Landing table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS landing (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            pattern_name TEXT,
            structure TEXT,
            cta_strategy TEXT,
            use_case TEXT,
            priority INTEGER
        )
    ''')
    
    # Charts table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS charts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            chart_type TEXT,
            use_case TEXT,
            library TEXT,
            priority INTEGER
        )
    ''')
    
    # UX Guidelines table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS ux_guidelines (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            guideline_name TEXT,
            description TEXT,
            anti_pattern TEXT,
            priority INTEGER
        )
    ''')
    
    # UI Reasoning table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS ui_reasoning (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_type TEXT,
            reasoning TEXT,
            priority INTEGER
        )
    ''')
    
    conn.commit()
    return conn

def import_csv_to_table(conn, csv_path, table_name, column_mapping):
    """Import CSV data into SQLite table."""
    cursor = conn.cursor()
    
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            columns = list(column_mapping.keys())
            values = [row.get(column_mapping[col], '') for col in columns]
            
            placeholders = ','.join(['?' for _ in columns])
            query = f"INSERT INTO {table_name} ({','.join(columns)}) VALUES ({placeholders})"
            
            cursor.execute(query, values)
    
    conn.commit()
    print(f"✅ Imported {csv_path} to {table_name}")

def main():
    data_dir = Path(__file__).parent.parent / 'data'
    db_path = data_dir / 'design-system.db'
    
    # Remove old database if exists
    if db_path.exists():
        db_path.unlink()
    
    print("Creating SQLite database...")
    conn = create_database(db_path)
    
    # Import each CSV file
    csv_mappings = {
        'products.csv': ('products', {
            'product_type': 'product_type',
            'pattern': 'pattern',
            'description': 'description',
            'priority': 'priority'
        }),
        'styles.csv': ('styles', {
            'style_name': 'style_name',
            'description': 'description',
            'colors': 'colors',
            'effects': 'effects',
            'use_cases': 'use_cases',
            'priority': 'priority'
        }),
        'typography.csv': ('typography', {
            'pairing_name': 'pairing_name',
            'heading_font': 'heading_font',
            'body_font': 'body_font',
            'use_case': 'use_case',
            'priority': 'priority'
        }),
        'colors.csv': ('colors', {
            'product_type': 'product_type',
            'palette_name': 'palette_name',
            'primary_color': 'primary_color',
            'secondary_color': 'secondary_color',
            'accent_color': 'accent_color',
            'priority': 'priority'
        }),
        'landing.csv': ('landing', {
            'pattern_name': 'pattern_name',
            'structure': 'structure',
            'cta_strategy': 'cta_strategy',
            'use_case': 'use_case',
            'priority': 'priority'
        }),
        'charts.csv': ('charts', {
            'chart_type': 'chart_type',
            'use_case': 'use_case',
            'library': 'library',
            'priority': 'priority'
        }),
        'ux-guidelines.csv': ('ux_guidelines', {
            'guideline_name': 'guideline_name',
            'description': 'description',
            'anti_pattern': 'anti_pattern',
            'priority': 'priority'
        }),
        'ui-reasoning.csv': ('ui_reasoning', {
            'product_type': 'product_type',
            'reasoning': 'reasoning',
            'priority': 'priority'
        })
    }
    
    for csv_file, (table_name, column_mapping) in csv_mappings.items():
        csv_path = data_dir / csv_file
        if csv_path.exists():
            import_csv_to_table(conn, csv_path, table_name, column_mapping)
    
    conn.close()
    
    # Get file sizes
    db_size = db_path.stat().st_size / 1024  # KB
    csv_total = sum((data_dir / csv).stat().st_size for csv in csv_mappings.keys() if (data_dir / csv).exists()) / 1024
    
    print(f"\n✅ Conversion complete!")
    print(f"📊 Database size: {db_size:.1f} KB")
    print(f"📊 Original CSV total: {csv_total:.1f} KB")
    print(f"💾 Space saved: {csv_total - db_size:.1f} KB ({(1 - db_size/csv_total)*100:.1f}%)")

if __name__ == '__main__':
    main()
