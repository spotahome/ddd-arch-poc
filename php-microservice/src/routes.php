<?php


$app->get('/[{eventId}]', function ($request, $response, $args) {
    
    
    
    $id = uniqid();

    $userPath = $this->avro['path'] . '/UserWasCreated.avro';
    $eventPath = $this->avro['path'] . '/Event.avro';

    $user = array(
        'id' => $id,
        'name' => 'name_' . $id,
        'email' => 'email_' . $id . '@domain.com'
    );

    $encodedUser = encode2Avro($userPath, $user, $this->logger);
    
    $event = array(
        'persistenceId' => $id,
        'eventId' => $args['eventId'],
        'creationDate' => date('Y-m-d H:i:s'),
        'tags' => array('UserWasCreatedEvent'),
        'payloadVersion' => '1',
        'payload' => base64_encode($encodedUser)
    );
    
    /*
    $encodedEvent = encode2Avro($eventPath, $event, $this->logger);
    $this->kafka->send([
        [
            'topic' => 'userevents',
            'value' => json_encode($event),
            'key' => $id,
        ],
    ]);
    */

    $uri = 'http://kafka-rest:10000/topics/avrotest2';

    $schemaContent = json_encode(json_decode(file_get_contents($eventPath)));

    $body = array(
        'value_schema' => $schemaContent,
        'records' => [
            0 => array('value' => $event)
        ]
    );

    $this->logger->info(json_encode($body));

    $response = \Httpful\Request::post($uri)

    ->body(json_encode($body))
    ->addHeaders(array(
        'Accept' => 'application/vnd.kafka.v2+json',
        'Content-Type' => 'application/vnd.kafka.avro.v2+json',
    ))
    ->send();

    $this->logger->info($response);
});

function encode2Avro($avroPath, $object, $l) {
    $userWasCreatedSchemaContent = file_get_contents($avroPath);
    $io = new AvroStringIO();

    $writersSchema = AvroSchema::parse($userWasCreatedSchemaContent);
    $l->info($avroPath);

    $writer = new AvroIODatumWriter($writersSchema);
    $dataWriter = new AvroDataIOWriter($io, $writer, $writersSchema);
    $dataWriter->append($object);
    $dataWriter->close();

    $binaryString = $io->string();
    return $binaryString;
}
