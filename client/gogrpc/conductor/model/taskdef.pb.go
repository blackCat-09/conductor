// Code generated by protoc-gen-go. DO NOT EDIT.
// source: model/taskdef.proto

package model // import "github.com/netflix/conductor/client/gogrpc/conductor/model"

import proto "github.com/golang/protobuf/proto"
import fmt "fmt"
import math "math"
import _struct "github.com/golang/protobuf/ptypes/struct"

// Reference imports to suppress errors if they are not otherwise used.
var _ = proto.Marshal
var _ = fmt.Errorf
var _ = math.Inf

// This is a compile-time assertion to ensure that this generated file
// is compatible with the proto package it is being compiled against.
// A compilation error at this line likely means your copy of the
// proto package needs to be updated.
const _ = proto.ProtoPackageIsVersion2 // please upgrade the proto package

type TaskDef_RetryLogic int32

const (
	TaskDef_FIXED               TaskDef_RetryLogic = 0
	TaskDef_EXPONENTIAL_BACKOFF TaskDef_RetryLogic = 1
)

var TaskDef_RetryLogic_name = map[int32]string{
	0: "FIXED",
	1: "EXPONENTIAL_BACKOFF",
}
var TaskDef_RetryLogic_value = map[string]int32{
	"FIXED":               0,
	"EXPONENTIAL_BACKOFF": 1,
}

func (x TaskDef_RetryLogic) String() string {
	return proto.EnumName(TaskDef_RetryLogic_name, int32(x))
}
func (TaskDef_RetryLogic) EnumDescriptor() ([]byte, []int) {
	return fileDescriptor_taskdef_9dd365e0d8e63269, []int{0, 0}
}

type TaskDef_TimeoutPolicy int32

const (
	TaskDef_RETRY       TaskDef_TimeoutPolicy = 0
	TaskDef_TIME_OUT_WF TaskDef_TimeoutPolicy = 1
	TaskDef_ALERT_ONLY  TaskDef_TimeoutPolicy = 2
)

var TaskDef_TimeoutPolicy_name = map[int32]string{
	0: "RETRY",
	1: "TIME_OUT_WF",
	2: "ALERT_ONLY",
}
var TaskDef_TimeoutPolicy_value = map[string]int32{
	"RETRY":       0,
	"TIME_OUT_WF": 1,
	"ALERT_ONLY":  2,
}

func (x TaskDef_TimeoutPolicy) String() string {
	return proto.EnumName(TaskDef_TimeoutPolicy_name, int32(x))
}
func (TaskDef_TimeoutPolicy) EnumDescriptor() ([]byte, []int) {
	return fileDescriptor_taskdef_9dd365e0d8e63269, []int{0, 1}
}

type TaskDef struct {
	Name                   string                    `protobuf:"bytes,1,opt,name=name" json:"name,omitempty"`
	Description            string                    `protobuf:"bytes,2,opt,name=description" json:"description,omitempty"`
	RetryCount             int32                     `protobuf:"varint,3,opt,name=retry_count,json=retryCount" json:"retry_count,omitempty"`
	TimeoutSeconds         int64                     `protobuf:"varint,4,opt,name=timeout_seconds,json=timeoutSeconds" json:"timeout_seconds,omitempty"`
	InputKeys              []string                  `protobuf:"bytes,5,rep,name=input_keys,json=inputKeys" json:"input_keys,omitempty"`
	OutputKeys             []string                  `protobuf:"bytes,6,rep,name=output_keys,json=outputKeys" json:"output_keys,omitempty"`
	TimeoutPolicy          TaskDef_TimeoutPolicy     `protobuf:"varint,7,opt,name=timeout_policy,json=timeoutPolicy,enum=com.netflix.conductor.proto.TaskDef_TimeoutPolicy" json:"timeout_policy,omitempty"`
	RetryLogic             TaskDef_RetryLogic        `protobuf:"varint,8,opt,name=retry_logic,json=retryLogic,enum=com.netflix.conductor.proto.TaskDef_RetryLogic" json:"retry_logic,omitempty"`
	RetryDelaySeconds      int32                     `protobuf:"varint,9,opt,name=retry_delay_seconds,json=retryDelaySeconds" json:"retry_delay_seconds,omitempty"`
	ResponseTimeoutSeconds int32                     `protobuf:"varint,10,opt,name=response_timeout_seconds,json=responseTimeoutSeconds" json:"response_timeout_seconds,omitempty"`
	ConcurrentExecLimit    int32                     `protobuf:"varint,11,opt,name=concurrent_exec_limit,json=concurrentExecLimit" json:"concurrent_exec_limit,omitempty"`
	InputTemplate          map[string]*_struct.Value `protobuf:"bytes,12,rep,name=input_template,json=inputTemplate" json:"input_template,omitempty" protobuf_key:"bytes,1,opt,name=key" protobuf_val:"bytes,2,opt,name=value"`
	XXX_NoUnkeyedLiteral   struct{}                  `json:"-"`
	XXX_unrecognized       []byte                    `json:"-"`
	XXX_sizecache          int32                     `json:"-"`
}

func (m *TaskDef) Reset()         { *m = TaskDef{} }
func (m *TaskDef) String() string { return proto.CompactTextString(m) }
func (*TaskDef) ProtoMessage()    {}
func (*TaskDef) Descriptor() ([]byte, []int) {
	return fileDescriptor_taskdef_9dd365e0d8e63269, []int{0}
}
func (m *TaskDef) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_TaskDef.Unmarshal(m, b)
}
func (m *TaskDef) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_TaskDef.Marshal(b, m, deterministic)
}
func (dst *TaskDef) XXX_Merge(src proto.Message) {
	xxx_messageInfo_TaskDef.Merge(dst, src)
}
func (m *TaskDef) XXX_Size() int {
	return xxx_messageInfo_TaskDef.Size(m)
}
func (m *TaskDef) XXX_DiscardUnknown() {
	xxx_messageInfo_TaskDef.DiscardUnknown(m)
}

var xxx_messageInfo_TaskDef proto.InternalMessageInfo

func (m *TaskDef) GetName() string {
	if m != nil {
		return m.Name
	}
	return ""
}

func (m *TaskDef) GetDescription() string {
	if m != nil {
		return m.Description
	}
	return ""
}

func (m *TaskDef) GetRetryCount() int32 {
	if m != nil {
		return m.RetryCount
	}
	return 0
}

func (m *TaskDef) GetTimeoutSeconds() int64 {
	if m != nil {
		return m.TimeoutSeconds
	}
	return 0
}

func (m *TaskDef) GetInputKeys() []string {
	if m != nil {
		return m.InputKeys
	}
	return nil
}

func (m *TaskDef) GetOutputKeys() []string {
	if m != nil {
		return m.OutputKeys
	}
	return nil
}

func (m *TaskDef) GetTimeoutPolicy() TaskDef_TimeoutPolicy {
	if m != nil {
		return m.TimeoutPolicy
	}
	return TaskDef_RETRY
}

func (m *TaskDef) GetRetryLogic() TaskDef_RetryLogic {
	if m != nil {
		return m.RetryLogic
	}
	return TaskDef_FIXED
}

func (m *TaskDef) GetRetryDelaySeconds() int32 {
	if m != nil {
		return m.RetryDelaySeconds
	}
	return 0
}

func (m *TaskDef) GetResponseTimeoutSeconds() int32 {
	if m != nil {
		return m.ResponseTimeoutSeconds
	}
	return 0
}

func (m *TaskDef) GetConcurrentExecLimit() int32 {
	if m != nil {
		return m.ConcurrentExecLimit
	}
	return 0
}

func (m *TaskDef) GetInputTemplate() map[string]*_struct.Value {
	if m != nil {
		return m.InputTemplate
	}
	return nil
}

func init() {
	proto.RegisterType((*TaskDef)(nil), "com.netflix.conductor.proto.TaskDef")
	proto.RegisterMapType((map[string]*_struct.Value)(nil), "com.netflix.conductor.proto.TaskDef.InputTemplateEntry")
	proto.RegisterEnum("com.netflix.conductor.proto.TaskDef_RetryLogic", TaskDef_RetryLogic_name, TaskDef_RetryLogic_value)
	proto.RegisterEnum("com.netflix.conductor.proto.TaskDef_TimeoutPolicy", TaskDef_TimeoutPolicy_name, TaskDef_TimeoutPolicy_value)
}

func init() { proto.RegisterFile("model/taskdef.proto", fileDescriptor_taskdef_9dd365e0d8e63269) }

var fileDescriptor_taskdef_9dd365e0d8e63269 = []byte{
	// 566 bytes of a gzipped FileDescriptorProto
	0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xff, 0x8c, 0x93, 0x4f, 0x6f, 0xd3, 0x40,
	0x10, 0xc5, 0xeb, 0xa6, 0x69, 0xc9, 0x84, 0xa6, 0x66, 0x23, 0x8a, 0x55, 0x40, 0x58, 0xbd, 0xe0,
	0x03, 0xb2, 0x51, 0x38, 0x50, 0x95, 0x53, 0xff, 0x38, 0x28, 0x6a, 0x68, 0x22, 0x63, 0xa0, 0xe5,
	0x80, 0xe5, 0x6c, 0x26, 0x66, 0x15, 0xdb, 0x6b, 0xd9, 0x6b, 0x54, 0x7f, 0x38, 0xbe, 0x1b, 0xda,
	0xb5, 0xd3, 0xa6, 0x20, 0xa1, 0xde, 0x76, 0xdf, 0x9b, 0x37, 0xc9, 0xfc, 0x76, 0x0c, 0xfd, 0x84,
	0xcf, 0x31, 0x76, 0x44, 0x58, 0x2c, 0xe7, 0xb8, 0xb0, 0xb3, 0x9c, 0x0b, 0x4e, 0x9e, 0x53, 0x9e,
	0xd8, 0x29, 0x8a, 0x45, 0xcc, 0x6e, 0x6c, 0xca, 0xd3, 0x79, 0x49, 0x05, 0xcf, 0x6b, 0xf3, 0xe0,
	0x45, 0xc4, 0x79, 0x14, 0xa3, 0xa3, 0x6e, 0xb3, 0x72, 0xe1, 0x14, 0x22, 0x2f, 0xa9, 0xa8, 0xdd,
	0xc3, 0xdf, 0xdb, 0xb0, 0xe3, 0x87, 0xc5, 0xf2, 0x1c, 0x17, 0x84, 0xc0, 0x56, 0x1a, 0x26, 0x68,
	0x68, 0xa6, 0x66, 0x75, 0x3c, 0x75, 0x26, 0x26, 0x74, 0xe7, 0x58, 0xd0, 0x9c, 0x65, 0x82, 0xf1,
	0xd4, 0xd8, 0x54, 0xd6, 0xba, 0x44, 0x5e, 0x41, 0x37, 0x47, 0x91, 0x57, 0x01, 0xe5, 0x65, 0x2a,
	0x8c, 0x96, 0xa9, 0x59, 0x6d, 0x0f, 0x94, 0x74, 0x26, 0x15, 0xf2, 0x1a, 0xf6, 0x04, 0x4b, 0x90,
	0x97, 0x22, 0x28, 0x50, 0xfe, 0xbb, 0xc2, 0xd8, 0x32, 0x35, 0xab, 0xe5, 0xf5, 0x1a, 0xf9, 0x73,
	0xad, 0x92, 0x97, 0x00, 0x2c, 0xcd, 0x4a, 0x11, 0x2c, 0xb1, 0x2a, 0x8c, 0xb6, 0xd9, 0xb2, 0x3a,
	0x5e, 0x47, 0x29, 0x17, 0x58, 0x15, 0xf2, 0x87, 0x78, 0x29, 0x6e, 0xfd, 0x6d, 0xe5, 0x43, 0x2d,
	0xa9, 0x82, 0x6b, 0x58, 0x75, 0x0c, 0x32, 0x1e, 0x33, 0x5a, 0x19, 0x3b, 0xa6, 0x66, 0xf5, 0x06,
	0x03, 0xfb, 0x3f, 0x7c, 0xec, 0x66, 0x7a, 0xdb, 0xaf, 0xa3, 0x53, 0x95, 0xf4, 0x76, 0xc5, 0xfa,
	0x95, 0x4c, 0x57, 0x43, 0xc6, 0x3c, 0x62, 0xd4, 0x78, 0xa4, 0xfa, 0x3a, 0x0f, 0xea, 0xeb, 0xc9,
	0xdc, 0x58, 0xc6, 0x1a, 0x2a, 0xea, 0x4c, 0x6c, 0xe8, 0xd7, 0x1d, 0xe7, 0x18, 0x87, 0xd5, 0x2d,
	0x99, 0x8e, 0xc2, 0xf7, 0x44, 0x59, 0xe7, 0xd2, 0x59, 0xc1, 0x39, 0x02, 0x23, 0xc7, 0x22, 0xe3,
	0x69, 0x81, 0xc1, 0xdf, 0x38, 0x41, 0x85, 0xf6, 0x57, 0xbe, 0x7f, 0x1f, 0xeb, 0x00, 0x9e, 0x52,
	0x9e, 0xd2, 0x32, 0xcf, 0x31, 0x15, 0x01, 0xde, 0x20, 0x0d, 0x62, 0x96, 0x30, 0x61, 0x74, 0x55,
	0xac, 0x7f, 0x67, 0xba, 0x37, 0x48, 0xc7, 0xd2, 0x22, 0x3f, 0xa0, 0x57, 0x3f, 0x85, 0xc0, 0x24,
	0x8b, 0x43, 0x81, 0xc6, 0x63, 0xb3, 0x65, 0x75, 0x07, 0xef, 0x1f, 0x34, 0xf2, 0x48, 0x46, 0xfd,
	0x26, 0xe9, 0xa6, 0x22, 0xaf, 0xbc, 0x5d, 0xb6, 0xae, 0x1d, 0x5c, 0x01, 0xf9, 0xb7, 0x88, 0xe8,
	0xd0, 0x5a, 0x62, 0xd5, 0xec, 0x9f, 0x3c, 0x92, 0x37, 0xd0, 0xfe, 0x15, 0xc6, 0x25, 0xaa, 0xc5,
	0xeb, 0x0e, 0xf6, 0xed, 0x7a, 0x99, 0xed, 0xd5, 0x32, 0xdb, 0x5f, 0xa5, 0xeb, 0xd5, 0x45, 0xc7,
	0x9b, 0x47, 0xda, 0xe1, 0x5b, 0x80, 0x3b, 0xe2, 0xa4, 0x03, 0xed, 0xe1, 0xe8, 0xca, 0x3d, 0xd7,
	0x37, 0xc8, 0x33, 0xe8, 0xbb, 0x57, 0xd3, 0xc9, 0xa5, 0x7b, 0xe9, 0x8f, 0x4e, 0xc6, 0xc1, 0xe9,
	0xc9, 0xd9, 0xc5, 0x64, 0x38, 0xd4, 0xb5, 0xc3, 0x0f, 0xb0, 0x7b, 0xef, 0xed, 0x65, 0xc8, 0x73,
	0x7d, 0xef, 0x5a, 0xdf, 0x20, 0x7b, 0xd0, 0xf5, 0x47, 0x9f, 0xdc, 0x60, 0xf2, 0xc5, 0x0f, 0xbe,
	0x0d, 0x75, 0x8d, 0xf4, 0x00, 0x4e, 0xc6, 0xae, 0xe7, 0x07, 0x93, 0xcb, 0xf1, 0xb5, 0xbe, 0x79,
	0xfa, 0xf1, 0xb4, 0xd3, 0x4c, 0x3d, 0x9d, 0x7d, 0x3f, 0x8e, 0x98, 0xf8, 0x59, 0xce, 0x24, 0x23,
	0xa7, 0x61, 0xe4, 0xdc, 0x32, 0x72, 0x68, 0xcc, 0x30, 0x15, 0x4e, 0xc4, 0xa3, 0x3c, 0xa3, 0x6b,
	0xba, 0xfa, 0xa2, 0x67, 0xdb, 0x6a, 0xa4, 0x77, 0x7f, 0x02, 0x00, 0x00, 0xff, 0xff, 0xe5, 0xb9,
	0x8e, 0x83, 0xe1, 0x03, 0x00, 0x00,
}
