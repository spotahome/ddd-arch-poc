<?php

$app->get('/[{name}]', function ($request, $response, $args) {
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
        'eventId' => '1',
        'creationDate' => '11/10/1981',
        'tags' => array('UserWasCreatedEvent'),
        'payloadVersion' => '1',
        'payload' => $encodedUser
    );


    $encodedEvent = encode2Avro($eventPath, $event, $this->logger);
    $this->logger->info($encodedEvent);
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
