from edna.core.execution.context import SimpleStreamingContext
from edna.ingest.streaming import KafkaIngest
from edna.process import BaseProcess
from edna.emit import KafkaEmit
from edna.serializers import StringSerializer
from edna.serializers import KafkaStringSerializer

from edna.ingest.streaming import SimulatedIngest
from edna.process.map import JsonToObject
from edna.serializers.EmptySerializer import EmptyStringSerializer
from edna.serializers.EmptySerializer import EmptyObjectSerializer

# This file will not work, because it ues only the Base Classes, which was empty.
# You can sue this as a starting point -- replace all the base classes with your needed classes

def main():
    list_of_inserts = ['{"first_name":"jessica", "last_name":"st. german", "additional":"unneeded1"}',
            '{"first_name":"jessica", "last_name":"courtney", "additional":"unneeded2"}',
            '{"first_name":"jessica", "last_name":"mishra", "additional":"unneeded3"}',
            '{"first_name":"jessica", "last_name":"novinha", "additional":"unneeded4"}',
            '{"first_name":"jessica", "last_name":"tudor", "additional":"unneeded5"}',
            '{"first_name":"jessica", "last_name":"ael-rayya", "additional":"unneeded6"}',
            '{"first_name":"jessica", "last_name":"zuma", "additional":"unneeded7"}',
            '{"first_name":"jessica", "last_name":"akihita", "additional":"unneeded8"}',
            '{"first_name":"jessica", "last_name":"xi", "additional":"unneeded9"}',
            '{"first_name":"jessica", "last_name":"kurylova", "additional":"unneeded10"}']

    context = SimpleStreamingContext()

    ingest = SimulatedIngest(serializer=EmptyStringSerializer, stream_list=list_of_inserts)

    emit_serializer = KafkaStringSerializer()    # e.g. StringSerializer

    process = BaseProcess()                     # e.g. BaseProcess
    emit = KafkaEmit(emit_serializer,
        kafka_topic=context.getVariable("export_key"),
        bootstrap_server=context.getVariable("bootstrap_server"))           # e.g. KafkaEmit

    context.addIngest(ingest=ingest)        # Registers the ingest primitive
    context.addProcess(process=process)     # Registers the process primitive
    context.addEmit(emit=emit)              # Registers the emit primitive

    context.execute()   # Executes  the context


if __name__ == "__main__":
    main()
