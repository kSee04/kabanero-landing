#!/bin/bash
# Exit immediately if a simple command exits with a non-zero status.
set -e
JEKYLL_BUILD_FLAGS=""
CONTENT_DIR="src/main/content"
WEBAPP_DIR="src/main/webapp"

echo "Ruby version:"
ruby -v

npm install
# Move carbon icons so jekyll will include them
CARBON_ICONS_DIR="$CONTENT_DIR/img/carbon-icons/"
mkdir -p $CARBON_ICONS_DIR
cp ./node_modules/carbon-icons/dist/carbon-icons.svg "$CONTENT_DIR/img/carbon-icons/"
cp ./node_modules/carbon-icons/dist/svg/* "$CONTENT_DIR/img/carbon-icons/"

./scripts/build_gem_dependencies.sh

# Guides that are ready to be published to the console
if [ "$TRAVIS_EVENT_TYPE" != "pull_request" ]; then 
    echo "Cloning guides..."
    ./scripts/build_clone_guides.sh "${GUIDES_GIT_URL}" "${GUIDES_GIT_REVISION}" "./ci/skip_guides"
fi

# Development environment only actions
if [ "$JEKYLL_ENV" != "production" ]; then 
    echo "Not in production environment..."
fi

# Only clone docs if they're not already there. Some builds clone the docs prior to this.
if [ ! -d "${CONTENT_DIR}/docs" ]; then
    ./scripts/build_clone_docs.sh "${DOCS_GIT_URL}" "${DOCS_GIT_REVISION}" # Argument is the repo and a revision of the docs repo
fi

# Development environments that enable the draft blogs in the _draft directory.
if [ "$JEKYLL_DRAFT_BLOGS" == "true" ]; then
    # Include draft blog posts for non production environments
    JEKYLL_BUILD_FLAGS="--drafts"
fi

if [ "$TRAVIS_EVENT_TYPE" != "pull_request" ]; then 

    echo "Copying guide images to /img/guide"
    mkdir -p "$CONTENT_DIR"/img/guide
    # Check if any published guide images exist first
    for GUIDE in $( ls "$CONTENT_DIR"/guides ); do
        if [[ -d "$CONTENT_DIR/guides/$GUIDE"/assets ]]; then
            cp "$CONTENT_DIR/guides/$GUIDE"/assets/* "$CONTENT_DIR"/img/guide/
        fi
    done

fi

# Build draft and published blogs
./scripts/build_clone_blogs.sh

# Jekyll build
echo "Building with jekyll..."
echo `jekyll -version`

# clean up older generated files to keep this script idempotent
find "$WEBAPP_DIR" -maxdepth 1 ! -name "webapp" ! -name 'WEB-INF' ! -name "META-INF" -exec rm -r {} +

# we put the generated files into src/main/webapp because the app server will host them properly on the root context.
# Previously we had them included as a webresource in the pom.xml, but the maven liberty plugin dev mode didn't include that
# which made development harder. 
#
# Jekyll will delete contents of the destination, so move them into webapp folder after.
bundle exec jekyll build $JEKYLL_BUILD_FLAGS --source "$CONTENT_DIR" --destination "$WEBAPP_DIR/jekyll-webapp"

mv "$WEBAPP_DIR"/jekyll-webapp/* "$WEBAPP_DIR"
rm -r "$WEBAPP_DIR"/jekyll-webapp

# Maven packaging
echo "Running maven (mvn)..."
mvn -B package
