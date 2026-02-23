import json
import time
from playwright.sync_api import sync_playwright

# Target accounts to scrape
TARGET_ACCOUNTS = ['ElonMusk', 'NASA', 'SpaceX']

def scrape_tweets():
    data = []
    
    with sync_playwright() as p:
        # Launch browser (headless=False so you can see it working and debug if needed)
        browser = p.chromium.launch(headless=False)
        context = browser.new_context()
        page = context.new_page()

        for handle in TARGET_ACCOUNTS:
            print(f"Scraping @{handle}...")
            try:
                page.goto(f"https://twitter.com/{handle}")
                page.wait_for_timeout(5000) # Wait for load using simple timeout to avoid detection/complexity

                # Scroll a bit to trigger lazy loading
                for _ in range(3):
                    page.mouse.wheel(0, 1000)
                    page.wait_for_timeout(2000)

                # Select all tweet articles
                tweets = page.locator('article[data-testid="tweet"]').all()
                print(f"Found {len(tweets)} tweets for @{handle}")

                for tweet in tweets[:5]: # XML Limit: scraped 5 per user for demo
                    try:
                        # Extract Text
                        text_el = tweet.locator('div[data-testid="tweetText"]')
                        text = text_el.inner_text() if text_el.count() > 0 else ""

                        # Extract Image
                        img_el = tweet.locator('div[data-testid="tweetPhoto"] img')
                        img_url = img_el.first.get_attribute('src') if img_el.count() > 0 else None

                        # Extract Timestamp (from time tag)
                        time_el = tweet.locator('time')
                        timestamp = time_el.get_attribute('datetime') if time_el.count() > 0 else None
                        
                        # Extract User Info (Avatar)
                        # This is tricky without dedicated selectors, relying on the fact that avatar is usually the first image in the tweet container area 
                        # or we can just use the profile picture from the header if we wanted, but let's try generic
                        
                        if text or img_url:
                            data.append({
                                "handle": handle,
                                "content": text,
                                "imageUrl": img_url,
                                "timestamp": timestamp,
                                "source": "X"
                            })
                    except Exception as e:
                        print(f"Error parsing tweet: {e}")
            
            except Exception as e:
                print(f"Failed to scrape {handle}: {e}")

        browser.close()

    # Save to JSON
    with open('../backend/src/main/resources/scraped_data.json', 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    print(f"Done! Saved {len(data)} tweets to backend resources.")

if __name__ == "__main__":
    scrape_tweets()
