#!/usr/bin/env python
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

# -*- encoding: utf-8 -*-

# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: Function.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
from google.protobuf import descriptor_pb2
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor.FileDescriptor(
  name='Function.proto',
  package='proto',
  syntax='proto3',
  serialized_pb=_b('\n\x0e\x46unction.proto\x12\x05proto\"\xdb\x05\n\x0e\x46unctionConfig\x12\x0e\n\x06tenant\x18\x01 \x01(\t\x12\x11\n\tnamespace\x18\x02 \x01(\t\x12\x0c\n\x04name\x18\x03 \x01(\t\x12\x11\n\tclassName\x18\x04 \x01(\t\x12\x0e\n\x06inputs\x18\x0e \x03(\t\x12G\n\x11\x63ustomSerdeInputs\x18\x05 \x03(\x0b\x32,.proto.FunctionConfig.CustomSerdeInputsEntry\x12\x1c\n\x14outputSerdeClassName\x18\x06 \x01(\t\x12\x0e\n\x06output\x18\x07 \x01(\t\x12H\n\x14processingGuarantees\x18\t \x01(\x0e\x32*.proto.FunctionConfig.ProcessingGuarantees\x12\x39\n\nuserConfig\x18\n \x03(\x0b\x32%.proto.FunctionConfig.UserConfigEntry\x12@\n\x10subscriptionType\x18\x0b \x01(\x0e\x32&.proto.FunctionConfig.SubscriptionType\x12.\n\x07runtime\x18\x0c \x01(\x0e\x32\x1d.proto.FunctionConfig.Runtime\x12\x0f\n\x07\x61utoAck\x18\r \x01(\x08\x1a\x38\n\x16\x43ustomSerdeInputsEntry\x12\x0b\n\x03key\x18\x01 \x01(\t\x12\r\n\x05value\x18\x02 \x01(\t:\x02\x38\x01\x1a\x31\n\x0fUserConfigEntry\x12\x0b\n\x03key\x18\x01 \x01(\t\x12\r\n\x05value\x18\x02 \x01(\t:\x02\x38\x01\"9\n\x14ProcessingGuarantees\x12\x0f\n\x0b\x41TMOST_ONCE\x10\x00\x12\x10\n\x0c\x41TLEAST_ONCE\x10\x01\"-\n\x10SubscriptionType\x12\n\n\x06SHARED\x10\x00\x12\r\n\tEXCLUSIVE\x10\x01\"\x1f\n\x07Runtime\x12\x08\n\x04JAVA\x10\x00\x12\n\n\x06PYTHON\x10\x01\".\n\x17PackageLocationMetaData\x12\x13\n\x0bpackagePath\x18\x01 \x01(\t\"\x9f\x01\n\x10\x46unctionMetaData\x12-\n\x0e\x66unctionConfig\x18\x01 \x01(\x0b\x32\x15.proto.FunctionConfig\x12\x37\n\x0fpackageLocation\x18\x02 \x01(\x0b\x32\x1e.proto.PackageLocationMetaData\x12\x0f\n\x07version\x18\x03 \x01(\x04\x12\x12\n\ncreateTime\x18\x04 \x01(\x04\"_\n\x08Snapshot\x12\x35\n\x14\x66unctionMetaDataList\x18\x01 \x03(\x0b\x32\x17.proto.FunctionMetaData\x12\x1c\n\x14lastAppliedMessageId\x18\x02 \x01(\x0c\"Q\n\nAssignment\x12\x31\n\x10\x66unctionMetaData\x18\x01 \x01(\x0b\x32\x17.proto.FunctionMetaData\x12\x10\n\x08workerId\x18\x02 \x01(\tB-\n!org.apache.pulsar.functions.protoB\x08\x46unctionb\x06proto3')
)



_FUNCTIONCONFIG_PROCESSINGGUARANTEES = _descriptor.EnumDescriptor(
  name='ProcessingGuarantees',
  full_name='proto.FunctionConfig.ProcessingGuarantees',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='ATMOST_ONCE', index=0, number=0,
      options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='ATLEAST_ONCE', index=1, number=1,
      options=None,
      type=None),
  ],
  containing_type=None,
  options=None,
  serialized_start=620,
  serialized_end=677,
)
_sym_db.RegisterEnumDescriptor(_FUNCTIONCONFIG_PROCESSINGGUARANTEES)

_FUNCTIONCONFIG_SUBSCRIPTIONTYPE = _descriptor.EnumDescriptor(
  name='SubscriptionType',
  full_name='proto.FunctionConfig.SubscriptionType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='SHARED', index=0, number=0,
      options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='EXCLUSIVE', index=1, number=1,
      options=None,
      type=None),
  ],
  containing_type=None,
  options=None,
  serialized_start=679,
  serialized_end=724,
)
_sym_db.RegisterEnumDescriptor(_FUNCTIONCONFIG_SUBSCRIPTIONTYPE)

_FUNCTIONCONFIG_RUNTIME = _descriptor.EnumDescriptor(
  name='Runtime',
  full_name='proto.FunctionConfig.Runtime',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='JAVA', index=0, number=0,
      options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='PYTHON', index=1, number=1,
      options=None,
      type=None),
  ],
  containing_type=None,
  options=None,
  serialized_start=726,
  serialized_end=757,
)
_sym_db.RegisterEnumDescriptor(_FUNCTIONCONFIG_RUNTIME)


_FUNCTIONCONFIG_CUSTOMSERDEINPUTSENTRY = _descriptor.Descriptor(
  name='CustomSerdeInputsEntry',
  full_name='proto.FunctionConfig.CustomSerdeInputsEntry',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='proto.FunctionConfig.CustomSerdeInputsEntry.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='proto.FunctionConfig.CustomSerdeInputsEntry.value', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=_descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('8\001')),
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=511,
  serialized_end=567,
)

_FUNCTIONCONFIG_USERCONFIGENTRY = _descriptor.Descriptor(
  name='UserConfigEntry',
  full_name='proto.FunctionConfig.UserConfigEntry',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='proto.FunctionConfig.UserConfigEntry.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='proto.FunctionConfig.UserConfigEntry.value', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=_descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('8\001')),
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=569,
  serialized_end=618,
)

_FUNCTIONCONFIG = _descriptor.Descriptor(
  name='FunctionConfig',
  full_name='proto.FunctionConfig',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='tenant', full_name='proto.FunctionConfig.tenant', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='namespace', full_name='proto.FunctionConfig.namespace', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='name', full_name='proto.FunctionConfig.name', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='className', full_name='proto.FunctionConfig.className', index=3,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='inputs', full_name='proto.FunctionConfig.inputs', index=4,
      number=14, type=9, cpp_type=9, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='customSerdeInputs', full_name='proto.FunctionConfig.customSerdeInputs', index=5,
      number=5, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='outputSerdeClassName', full_name='proto.FunctionConfig.outputSerdeClassName', index=6,
      number=6, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='output', full_name='proto.FunctionConfig.output', index=7,
      number=7, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='processingGuarantees', full_name='proto.FunctionConfig.processingGuarantees', index=8,
      number=9, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='userConfig', full_name='proto.FunctionConfig.userConfig', index=9,
      number=10, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='subscriptionType', full_name='proto.FunctionConfig.subscriptionType', index=10,
      number=11, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='runtime', full_name='proto.FunctionConfig.runtime', index=11,
      number=12, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='autoAck', full_name='proto.FunctionConfig.autoAck', index=12,
      number=13, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_FUNCTIONCONFIG_CUSTOMSERDEINPUTSENTRY, _FUNCTIONCONFIG_USERCONFIGENTRY, ],
  enum_types=[
    _FUNCTIONCONFIG_PROCESSINGGUARANTEES,
    _FUNCTIONCONFIG_SUBSCRIPTIONTYPE,
    _FUNCTIONCONFIG_RUNTIME,
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=26,
  serialized_end=757,
)


_PACKAGELOCATIONMETADATA = _descriptor.Descriptor(
  name='PackageLocationMetaData',
  full_name='proto.PackageLocationMetaData',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='packagePath', full_name='proto.PackageLocationMetaData.packagePath', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=759,
  serialized_end=805,
)


_FUNCTIONMETADATA = _descriptor.Descriptor(
  name='FunctionMetaData',
  full_name='proto.FunctionMetaData',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='functionConfig', full_name='proto.FunctionMetaData.functionConfig', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='packageLocation', full_name='proto.FunctionMetaData.packageLocation', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='version', full_name='proto.FunctionMetaData.version', index=2,
      number=3, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='createTime', full_name='proto.FunctionMetaData.createTime', index=3,
      number=4, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=808,
  serialized_end=967,
)


_SNAPSHOT = _descriptor.Descriptor(
  name='Snapshot',
  full_name='proto.Snapshot',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='functionMetaDataList', full_name='proto.Snapshot.functionMetaDataList', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='lastAppliedMessageId', full_name='proto.Snapshot.lastAppliedMessageId', index=1,
      number=2, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value=_b(""),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=969,
  serialized_end=1064,
)


_ASSIGNMENT = _descriptor.Descriptor(
  name='Assignment',
  full_name='proto.Assignment',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='functionMetaData', full_name='proto.Assignment.functionMetaData', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='workerId', full_name='proto.Assignment.workerId', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=1066,
  serialized_end=1147,
)

_FUNCTIONCONFIG_CUSTOMSERDEINPUTSENTRY.containing_type = _FUNCTIONCONFIG
_FUNCTIONCONFIG_USERCONFIGENTRY.containing_type = _FUNCTIONCONFIG
_FUNCTIONCONFIG.fields_by_name['customSerdeInputs'].message_type = _FUNCTIONCONFIG_CUSTOMSERDEINPUTSENTRY
_FUNCTIONCONFIG.fields_by_name['processingGuarantees'].enum_type = _FUNCTIONCONFIG_PROCESSINGGUARANTEES
_FUNCTIONCONFIG.fields_by_name['userConfig'].message_type = _FUNCTIONCONFIG_USERCONFIGENTRY
_FUNCTIONCONFIG.fields_by_name['subscriptionType'].enum_type = _FUNCTIONCONFIG_SUBSCRIPTIONTYPE
_FUNCTIONCONFIG.fields_by_name['runtime'].enum_type = _FUNCTIONCONFIG_RUNTIME
_FUNCTIONCONFIG_PROCESSINGGUARANTEES.containing_type = _FUNCTIONCONFIG
_FUNCTIONCONFIG_SUBSCRIPTIONTYPE.containing_type = _FUNCTIONCONFIG
_FUNCTIONCONFIG_RUNTIME.containing_type = _FUNCTIONCONFIG
_FUNCTIONMETADATA.fields_by_name['functionConfig'].message_type = _FUNCTIONCONFIG
_FUNCTIONMETADATA.fields_by_name['packageLocation'].message_type = _PACKAGELOCATIONMETADATA
_SNAPSHOT.fields_by_name['functionMetaDataList'].message_type = _FUNCTIONMETADATA
_ASSIGNMENT.fields_by_name['functionMetaData'].message_type = _FUNCTIONMETADATA
DESCRIPTOR.message_types_by_name['FunctionConfig'] = _FUNCTIONCONFIG
DESCRIPTOR.message_types_by_name['PackageLocationMetaData'] = _PACKAGELOCATIONMETADATA
DESCRIPTOR.message_types_by_name['FunctionMetaData'] = _FUNCTIONMETADATA
DESCRIPTOR.message_types_by_name['Snapshot'] = _SNAPSHOT
DESCRIPTOR.message_types_by_name['Assignment'] = _ASSIGNMENT
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

FunctionConfig = _reflection.GeneratedProtocolMessageType('FunctionConfig', (_message.Message,), dict(

  CustomSerdeInputsEntry = _reflection.GeneratedProtocolMessageType('CustomSerdeInputsEntry', (_message.Message,), dict(
    DESCRIPTOR = _FUNCTIONCONFIG_CUSTOMSERDEINPUTSENTRY,
    __module__ = 'Function_pb2'
    # @@protoc_insertion_point(class_scope:proto.FunctionConfig.CustomSerdeInputsEntry)
    ))
  ,

  UserConfigEntry = _reflection.GeneratedProtocolMessageType('UserConfigEntry', (_message.Message,), dict(
    DESCRIPTOR = _FUNCTIONCONFIG_USERCONFIGENTRY,
    __module__ = 'Function_pb2'
    # @@protoc_insertion_point(class_scope:proto.FunctionConfig.UserConfigEntry)
    ))
  ,
  DESCRIPTOR = _FUNCTIONCONFIG,
  __module__ = 'Function_pb2'
  # @@protoc_insertion_point(class_scope:proto.FunctionConfig)
  ))
_sym_db.RegisterMessage(FunctionConfig)
_sym_db.RegisterMessage(FunctionConfig.CustomSerdeInputsEntry)
_sym_db.RegisterMessage(FunctionConfig.UserConfigEntry)

PackageLocationMetaData = _reflection.GeneratedProtocolMessageType('PackageLocationMetaData', (_message.Message,), dict(
  DESCRIPTOR = _PACKAGELOCATIONMETADATA,
  __module__ = 'Function_pb2'
  # @@protoc_insertion_point(class_scope:proto.PackageLocationMetaData)
  ))
_sym_db.RegisterMessage(PackageLocationMetaData)

FunctionMetaData = _reflection.GeneratedProtocolMessageType('FunctionMetaData', (_message.Message,), dict(
  DESCRIPTOR = _FUNCTIONMETADATA,
  __module__ = 'Function_pb2'
  # @@protoc_insertion_point(class_scope:proto.FunctionMetaData)
  ))
_sym_db.RegisterMessage(FunctionMetaData)

Snapshot = _reflection.GeneratedProtocolMessageType('Snapshot', (_message.Message,), dict(
  DESCRIPTOR = _SNAPSHOT,
  __module__ = 'Function_pb2'
  # @@protoc_insertion_point(class_scope:proto.Snapshot)
  ))
_sym_db.RegisterMessage(Snapshot)

Assignment = _reflection.GeneratedProtocolMessageType('Assignment', (_message.Message,), dict(
  DESCRIPTOR = _ASSIGNMENT,
  __module__ = 'Function_pb2'
  # @@protoc_insertion_point(class_scope:proto.Assignment)
  ))
_sym_db.RegisterMessage(Assignment)


DESCRIPTOR.has_options = True
DESCRIPTOR._options = _descriptor._ParseOptions(descriptor_pb2.FileOptions(), _b('\n!org.apache.pulsar.functions.protoB\010Function'))
_FUNCTIONCONFIG_CUSTOMSERDEINPUTSENTRY.has_options = True
_FUNCTIONCONFIG_CUSTOMSERDEINPUTSENTRY._options = _descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('8\001'))
_FUNCTIONCONFIG_USERCONFIGENTRY.has_options = True
_FUNCTIONCONFIG_USERCONFIGENTRY._options = _descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('8\001'))
# @@protoc_insertion_point(module_scope)
