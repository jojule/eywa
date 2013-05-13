# Eywa Add-on for Vaadin 7

Eywa is a shared in-memory datasource component add-on for Vaadin 7. Its goal is to provice easy to use Property, Item and Container implementations that share data safely between multiple users. 

## Download release

Official releases of this add-on are available at Vaadin Directory. For Maven instructions, download and reviews, go to http://vaadin.com/addon/eywa

## Building and running demo

git clone http://github.com/jojule/eywa
mvn clean install
cd demo
mvn jetty:run

To see the demo, navigate to http://localhost:8080/

## Development with Eclipse IDE

For further development of this add-on, the following tool-chain is recommended:
- Eclipse IDE
- m2e wtp plug-in (install it from Eclipse Marketplace)
- Vaadin Eclipse plug-in (install it from Eclipse Marketplace)
- Chrome browser

### Importing project

Choose File > Import... > Existing Maven Projects


## Release notes

### Version 0.0.1-SNAPSHOT
- ...
- ...

## Issue tracking

The issues for this add-on are tracked on its github.com page. All bug reports and feature requests are appreciated. 

## Contributions

Contributions are welcome, but there are no guarantees that they are accepted as such. Process for contributing is the following:
- Fork this project
- Create an issue to this project about the contribution (bug or feature) if there is no such issue about it already. Try to keep the scope minimal.
- Develop and test the fix or functionality carefully. Only include minimum amount of code needed to fix the issue.
- Refer to the fixed issue in commit
- Send a pull request for the original project
- Comment on the original issue that you have implemented a fix for it

## License & Author

Add-on is distributed under Apache License 2.0. For license terms, see LICENSE.txt.

Eywa is written by Joonas Lehtinen

# Developer Guide

## Getting started

Here is a simple example on how to try out the add-on component:

TextField tf = new TextField("Shared textfield");
tf.setPropertyDataSource(new EywaProperty<String>("gid", String.class));

