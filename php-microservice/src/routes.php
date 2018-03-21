<?php


$app->get('/[{eventId}]', function ($request, $response, $args) {
    $id = uniqid();

    $userPath = $this->avro['path'] . '/UserWasCreated.avro';
    $eventPath = $this->avro['path'] . '/Event.avro';

/*
    $user = array(
        'id' => $id,
        'name' => 'name_' . $id,
        'email' => 'email_' . $id . '@domain.com'
    );
    */

    $user = array(
        'id' => '1',
        'name' => 'name_',
        'email' => 'email_'
    );


    $encodedUser = encode2Avro($userPath, $user, $this->logger);

    $this->logger->info('>>>>>>>>>>>>>>>> ' . $encodedUser);
    
    $event = array(
        'persistenceId' => '1',//$id,
        'eventId' => $args['eventId'],
        'creationDate' => '1',//date('Y-m-d H:i:s'),
        'payloadName' => '1',//'UserWasCreatedEvent',
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

    $uri = 'http://kafka-rest:10000/topics/userevents';

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
    $l->info($avroPath);

    $writer = new AvroIODatumWriter($writersSchema);
    $dataWriter = new AvroDataIOWriter($io, $writer, $writersSchema);
    $dataWriter->append($object);
    $dataWriter->close();

    $binaryString = $io->string();
    return $binaryString;
}
