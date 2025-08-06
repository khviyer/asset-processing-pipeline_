
import os
import json
import hashlib
import zipfile
from PIL import Image

ASSETS_DIR = 'assets'
OUTPUT_DIR = 'output'

def calculate_md5(filepath):
    with open(filepath, 'rb') as f:
        return hashlib.md5(f.read()).hexdigest()

def process_solo_image(image_path):
    print(f"Processing solo image: {image_path}")
    base_name = os.path.basename(image_path)
    zip_name = os.path.join(OUTPUT_DIR, f"{os.path.splitext(base_name)[0]}.zip")
    with zipfile.ZipFile(zip_name, 'w') as zf:
        zf.write(image_path, base_name)
    
    md5_hash = calculate_md5(zip_name)
    final_zip_name = os.path.join(OUTPUT_DIR, f"{md5_hash}.zip")
    os.rename(zip_name, final_zip_name)
    print(f"Created solo image archive: {final_zip_name}")

def process_bundle(image_path, json_path):
    print(f"Processing bundle: {image_path}, {json_path}")
    base_name = os.path.splitext(os.path.basename(image_path))[0]
    
    with open(json_path, 'r') as f:
        config = json.load(f)
    
    rotate_angle = 0
    rotate_type = config.get('rotate', 'none').lower()
    
    if rotate_type == 'left':
        rotate_angle = 90
    elif rotate_type == 'right':
        rotate_angle = -90  # or 270
    
    img = Image.open(image_path)
    if rotate_angle != 0:
        img = img.rotate(rotate_angle, expand=True)
    
    png_image_path = os.path.join(OUTPUT_DIR, f"{base_name}.png")
    img.save(png_image_path, 'PNG')
    print(f"Converted and rotated image to PNG: {png_image_path}")
    
    zip_name = os.path.join(OUTPUT_DIR, f"{base_name}.zip")
    with zipfile.ZipFile(zip_name, 'w') as zf:
        zf.write(png_image_path, os.path.basename(png_image_path))
        zf.write(json_path, os.path.basename(json_path))

    md5_hash = calculate_md5(zip_name)
    final_zip_name = os.path.join(OUTPUT_DIR, f"{md5_hash}.zip")
    os.rename(zip_name, final_zip_name)
    print(f"Created bundle archive: {final_zip_name}")

def main():
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)

    assets = {}
    for f in os.listdir(ASSETS_DIR):
        name, ext = os.path.splitext(f)
        path = os.path.join(ASSETS_DIR, f)
        if ext.lower() in ['.png', '.jpg', '.jpeg']:
            if name not in assets:
                assets[name] = {'image': path}
            else:
                assets[name]['image'] = path
        elif ext.lower() == '.json':
            if name not in assets:
                assets[name] = {'json': path}
            else:
                assets[name]['json'] = path

    for name, files in assets.items():
        if 'image' in files and 'json' in files:
            process_bundle(files['image'], files['json'])
        elif 'image' in files and 'json' not in files:
            process_solo_image(files['image'])
        else:
            print(f"Skipping {name}: Incomplete asset (missing image or json).")

if __name__ == "__main__":
    main()


