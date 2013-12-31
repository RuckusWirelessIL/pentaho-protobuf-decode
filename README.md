pentaho-protobuf-decode
=======================

Google Protocol Buffers message decoder for Pentaho Kettle.
This step allows accessing standalone fields from objects encoded using Google Protocol Buffers.

[![Build Status](https://travis-ci.org/RuckusWirelessIL/pentaho-protobuf-decode.png)](https://travis-ci.org/RuckusWirelessIL/pentaho-protobuf-decode)


### Screenshots ###

Following is the example of live decoding of Protocol Buffers message transfered via Apache Kafka:

![Live decoding of Protocol Buffers message transfered via Apache Kafka](https://raw.github.com/RuckusWirelessIL/pentaho-pentaho-protobuf-decode/master/doc/example.png)


### Building ###

```
mvn clean package
```

### Installation ###

Extract ```target/pentaho-protobuf-decode-1.0.0.zip``` in *plugins/steps* directory of your Pentaho Data Integration distribution.

