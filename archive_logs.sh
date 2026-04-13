#!/bin/bash

# Configuration
SOURCE_DIR="/var/log/cms"
ARCHIVE_DIR="/var/log/ott-cms/archive"
MAX_SIZE_MB=5
DATE_STR=$(date +%Y-%m-%d)
EMAILS="hakan@saatteknoloji.com berkaydemirbas@saatteknoloji.com"
LOG_FILE="/tmp/archive_process_$DATE_STR.log"

# Create archive directory if it doesn't exist
mkdir -p "$ARCHIVE_DIR"

echo "Log Archiving Process Started: $(date)" | tee "$LOG_FILE"
echo "------------------------------------------" | tee -a "$LOG_FILE"

# Counter for archived files
ARCHIVED_COUNT=0
MOVED_FILES=""

# Check if source directory exists
if [ ! -d "$SOURCE_DIR" ]; then
    echo "Error: Source directory $SOURCE_DIR does not exist." | tee -a "$LOG_FILE"
    exit 1
fi

# Find files larger than 5MB and process them
# Using find with -size +5M (approximate)
while IFS= read -r file; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        extension="${filename##*.}"
        basename="${filename%.*}"
        
        new_filename="${basename}_${DATE_STR}.${extension}"
        target_path="$ARCHIVE_DIR/$new_filename"
        
        echo "Processing: $filename (Size: $(du -h "$file" | cut -f1))" | tee -a "$LOG_FILE"
        
        # Move and rename
        mv "$file" "$target_path"
        
        # Zip the file
        zip -j "$target_path.zip" "$target_path" > /dev/null
        rm "$target_path"
        
        MOVED_FILES="$MOVED_FILES\n- $new_filename.zip"
        ((ARCHIVED_COUNT++))
    fi
done < <(find "$SOURCE_DIR" -maxdepth 1 -name "*.log" -size +"$MAX_SIZE_MB"M)

echo "------------------------------------------" | tee -a "$LOG_FILE"
echo "Process Finished." | tee -a "$LOG_FILE"
echo "Total Files Archived: $ARCHIVED_COUNT" | tee -a "$LOG_FILE"
echo -e "Archived Files: $MOVED_FILES" | tee -a "$LOG_FILE"

# Send Email (Assuming 'mail' or 'sendmail' is configured)
# If using a specific SMTP tool, this part would be adjusted.
SUBJECT="CMS Log Archive Report - $DATE_STR"
BODY=$(cat "$LOG_FILE")

# Sending email to recipients
for email in $EMAILS; do
    echo "Sending report to $email..."
    echo -e "Subject: $SUBJECT\n\n$BODY" | mail -s "$SUBJECT" "$email"
done

echo "Report sent to: $EMAILS"
