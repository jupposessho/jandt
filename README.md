# Developers Connected

## Running

To run the application, run

`sbt "connected/run"` 

or

`sbt "connected/pack"` and then `connected/target/pack/bin/main`

The only endpoint is:

`GET /developers/connected/dev1/dev2`

## Running Tests

From the terminal, run `sbt test` for Unit Tests

## Environment variables

- TWITTER_CONSUMER_KEY: consumer key for twitter
- TWITTER_CONSUMER_SECRET: consumer secret for twitter
- TWITTER_ACCESS_KEY: access key for twitter
- TWITTER_ACCESS_SECRET: access secret for twitter
- TWITTER_RETRY_COUNT: how many times should the client retry the request to twitter on failure
- TWITTER_RETRY_DURATION: initial delay for retry
- GITHUB_API_URL: github api url
- GITHUB_RETRY_COUNT: how many times should the client retry the request to github on failure
- GITHUB_RETRY_DURATION:  initial delay for retry