<?php


$app->get('/[{eventId}]', function ($request, $response, $args) {
    $uri = 'http://kafka-rest:10000/topics/events.user';
    
    $eventId = uniqid();
    $userId = uniqid();

    $userPath = $this->avro['path'] . '/UserWasCreated.avro';
    $eventPath = $this->avro['path'] . '/Event.avro';

    $user = array(
        'id' => $userId,
        'name' => 'name_' . $userId,
        'email' => 'email_' . $userId . '@domain.com'
    );

    $encodedUser = encode2Avro($userPath, $user, $this->logger);
    
    $event = array(
        'persistenceId' => $userId,
        'eventId' => $eventId,
        'creationDate' => date('Y-m-d H:i:s'),
        'payloadName' => 'UserWasCreatedEvent',
        'payloadVersion' => '1',
        'payload' => base64_encode($encodedUser)
    );

    $schemaContent = json_encode(json_decode(file_get_contents($eventPath)));

    $body = array(
        'value_schema' => $schemaContent,
        'records' => [
            0 => array('value' => $event)
        ]
    );

    $response = \Httpful\Request::post($uri)
    ->body(json_encode($body))
    ->addHeaders(array(
        'Accept' => 'application/vnd.kafka.v2+json',
        'Content-Type' => 'application/vnd.kafka.avro.v2+json',
    ))
    ->send();
});

function encode2Avro($avroPath, $object, $l) {
    $userWasCreatedSchemaContent = file_get_contents($avroPath);
    $io = new AvroStringIO();

    $writersSchema = AvroSchema::parse($userWasCreatedSchemaContent);

    $writer = new AvroIODatumWriter($writersSchema);
    $encoder = new AvroIOBinaryEncoder($io);
    $writer->write($object, $encoder);

    return $io->string();
}
