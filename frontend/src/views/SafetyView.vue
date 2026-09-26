<template>
  <div class="page">
    <div class="page-head">
      <h2 class="page-title">安全停射联锁台 <em>停射事件 → 双人分项复核 → 复射放行</em></h2>
      <div class="page-tools">
        <el-radio-group v-model="role" size="small">
          <el-radio-button v-for="r in options.roles || []" :key="r.code" :value="r.code">{{ r.name }}</el-radio-button>
        </el-radio-group>
        <el-input v-model="operatorName" placeholder="操作人姓名" style="width: 130px" size="small" />
        <el-button size="small" @click="reloadAll">刷新</el-button>
        <el-button v-if="isManager" type="danger" size="small" @click="openCreate">建立停射事件</el-button>
      </div>
    </div>

    <div class="rule-tip">
      值班经理建立停射事件后，受影响箭道立即安全锁定（不可开台 / 开新回合）、进行中回合暂停（保留箭支与分数）、
      器材冻结（不可租借 / 归还 / 报修）。教练确认射线与人员安全、器材管理员确认器材已检查，
      两项复核都通过后值班经理才能复射放行；任一复核不通过，事件继续锁定。事件创建人不能代替复核角色签字。
    </div>

    <div class="interlock">
      <!-- 左：事件列表 -->
      <div class="panel list-panel">
        <div class="panel-title">
          停射事件
          <small>在办 {{ activeCount }} 起 · 共 {{ events.length }} 起</small>
        </div>
        <el-select v-model="filterStatus" clearable placeholder="按状态筛选" size="small" style="width: 100%; margin-bottom: 8px" @change="loadEvents">
          <el-option label="待处置" value="PENDING" />
          <el-option label="分项复核" value="REVIEWING" />
          <el-option label="已放行" value="RELEASED" />
        </el-select>
        <div class="event-list">
          <div
            v-for="item in events"
            :key="item.id"
            class="event-card"
            :class="{ 'is-on': currentId === item.id }"
            @click="selectEvent(item.id)"
          >
            <div class="event-card-head">
              <span class="mono event-no">{{ item.eventNo }}</span>
              <el-tag size="small" :type="statusTagType(item.status)" effect="dark">{{ item.statusName }}</el-tag>
            </div>
            <div class="event-card-title">
              <el-tag size="small" :type="severityTagType(item.severity)" effect="plain">{{ item.severityName }}</el-tag>
              <span>{{ item.eventTypeName }}</span>
            </div>
            <div class="event-card-sub muted">{{ item.reason }}</div>
            <div class="event-card-foot muted">
              {{ item.createdBy }} · {{ shortTime(item.createdAt) }} ·
              道 {{ item.laneCount }} / 械 {{ item.equipmentCount }} / 回合 {{ item.roundCount }}
            </div>
          </div>
          <el-empty v-if="!events.length" description="暂无停射事件" :image-size="60" />
        </div>
      </div>

      <!-- 右：事件详情 -->
      <div v-if="detail" class="detail">
        <div class="panel">
          <div class="panel-title">
            事件 {{ detail.event.eventNo }}
            <el-tag size="small" :type="statusTagType(detail.event.status)" effect="dark">{{ detail.event.statusName }}</el-tag>
            <el-tag size="small" :type="severityTagType(detail.event.severity)" effect="plain">{{ detail.event.severityName }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ detail.event.eventTypeName }}</el-tag>
          </div>
          <div class="kv-grid">
            <div class="kv"><span class="kv-k">停射原因</span><span class="kv-v">{{ detail.event.reason }}</span></div>
            <div class="kv"><span class="kv-k">现场说明</span><span class="kv-v">{{ detail.event.description }}</span></div>
            <div class="kv"><span class="kv-k">建立</span><span class="kv-v">{{ detail.event.createdBy }}（值班经理） · {{ shortTime(detail.event.createdAt) }}</span></div>
            <div class="kv">
              <span class="kv-k">放行</span>
              <span class="kv-v">
                <template v-if="detail.event.releasedAt">{{ detail.event.releasedBy }}（值班经理） · {{ shortTime(detail.event.releasedAt) }}</template>
                <span v-else class="muted">未放行</span>
              </span>
            </div>
          </div>

          <!-- 值班经理动作区：只向当前角色展示允许执行的动作 -->
          <div v-if="isManager && detail.event.status !== 'RELEASED'" class="action-bar">
            <el-button
              v-if="detail.event.status === 'PENDING'"
              type="primary"
              :loading="acting"
              :disabled="acting"
              @click="doAdvance"
            >转入分项复核</el-button>
            <template v-if="detail.event.status === 'REVIEWING'">
              <el-button
                type="success"
                :loading="acting"
                :disabled="acting || !bothPassed"
                @click="doRelease"
              >复射放行</el-button>
              <span v-if="!bothPassed" class="muted">两项复核都「通过」后才能放行</span>
            </template>
          </div>
          <div v-if="detail.event.status === 'REVIEWING' && anyRejected" class="reject-tip">
            存在复核不通过项，事件继续锁定，不能放行。
          </div>
        </div>

        <!-- 受影响资源 -->
        <div class="panel">
          <div class="panel-title">受影响资源 <small>锁定前状态已快照，放行时整体恢复</small></div>
          <div class="res-groups">
            <div class="res-group">
              <div class="res-title">箭道 · {{ detail.lanes.length }}</div>
              <template v-if="detail.lanes.length">
                <div v-for="l in detail.lanes" :key="l.laneId" class="res-item">
                  <span class="mono">{{ l.laneNo }}</span>
                  <el-tag size="small" type="danger" effect="dark">{{ l.restored ? '已解除锁定' : '安全锁定' }}</el-tag>
                  <span class="muted">锁定前：{{ l.prevStatusName }}</span>
                </div>
              </template>
              <span v-else class="muted">未涉及箭道</span>
            </div>
            <div class="res-group">
              <div class="res-title">器材 · {{ detail.equipment.length }}</div>
              <template v-if="detail.equipment.length">
                <div v-for="e in detail.equipment" :key="e.equipmentId" class="res-item">
                  <span class="mono">{{ e.equipCode }}</span>
                  <el-tag size="small" type="danger" effect="dark">{{ e.restored ? '已解除锁定' : '安全锁定' }}</el-tag>
                  <span class="muted">
                    锁定前：{{ e.prevStatusName }}<template v-if="e.prevRenterName">（{{ e.prevRenterName }}）</template>
                  </span>
                </div>
              </template>
              <span v-else class="muted">未涉及器材</span>
            </div>
            <div class="res-group">
              <div class="res-title">暂停回合 · {{ detail.rounds.length }}</div>
              <template v-if="detail.rounds.length">
                <div v-for="r in detail.rounds" :key="r.roundId" class="res-item">
                  <span class="mono">{{ r.roundNo }}</span>
                  <el-tag size="small" :type="r.restored ? 'success' : 'warning'" effect="dark">
                    {{ r.restored ? '已恢复进行' : '暂停中' }}
                  </el-tag>
                  <span class="muted">{{ r.memberName }} · {{ r.laneNo }} 道 · 进度 {{ r.shotCount }}/{{ r.arrowCount }}</span>
                </div>
              </template>
              <span v-else class="muted">无被暂停的回合</span>
            </div>
          </div>
        </div>

        <!-- 分项复核 -->
        <div class="panel">
          <div class="panel-title">分项复核 <small>每项复核只有一份最终结论；创建人不能代签</small></div>
          <div class="review-grid">
            <div v-for="rt in reviewTypes" :key="rt.code" class="review-card">
              <div class="review-head">
                <b>{{ rt.name }}</b>
                <span class="muted">{{ rt.code === 'COACH' ? '确认射线与人员已安全' : '确认相关器材已检查' }}</span>
              </div>
              <template v-if="reviewOf(rt.code)">
                <div class="review-result">
                  <el-tag size="small" :type="reviewOf(rt.code).conclusion === 'PASS' ? 'success' : 'danger'" effect="dark">
                    {{ reviewOf(rt.code).conclusionName }}
                  </el-tag>
                  <span>{{ reviewOf(rt.code).reviewerName }}</span>
                  <span class="muted">{{ shortTime(reviewOf(rt.code).reviewedAt) }}</span>
                </div>
                <div v-if="reviewOf(rt.code).note" class="review-note">「{{ reviewOf(rt.code).note }}」</div>
              </template>
              <template v-else>
                <div class="muted review-pending">待复核</div>
                <div v-if="canReview(rt.code)" class="review-form">
                  <el-radio-group v-model="reviewForms[rt.code].conclusion" size="small">
                    <el-radio-button value="PASS">通过</el-radio-button>
                    <el-radio-button value="REJECT">不通过</el-radio-button>
                  </el-radio-group>
                  <el-input
                    v-model="reviewForms[rt.code].note"
                    size="small"
                    :placeholder="reviewForms[rt.code].conclusion === 'REJECT' ? '不通过原因（必填，事件继续锁定）' : '复核说明（选填）'"
                  />
                  <el-button
                    size="small"
                    type="primary"
                    :loading="acting"
                    :disabled="acting"
                    @click="doReview(rt.code)"
                  >提交复核结论</el-button>
                </div>
                <div v-else-if="detail.event.status === 'REVIEWING' && isCreator" class="muted review-pending">
                  你是事件创建人，不能代替复核角色签字
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- 状态轨迹 -->
        <div class="panel">
          <div class="panel-title">状态轨迹 <small>创建 / 复核 / 放行全程留痕</small></div>
          <el-timeline class="trail">
            <el-timeline-item
              v-for="log in detail.logs"
              :key="log.createdAt + log.action + log.operatorName"
              :timestamp="shortTime(log.createdAt)"
              :type="log.action === 'RELEASE' ? 'success' : log.action.startsWith('REVIEW') ? 'primary' : 'warning'"
            >
              <div class="trail-line">
                <b>{{ actionName(log.action) }}</b>
                <span class="muted">{{ log.operatorName }}（{{ log.operatorRoleName }}）</span>
                <el-tag v-if="log.toStatus && log.fromStatus !== log.toStatus" size="small" effect="plain" type="info">
                  {{ log.fromStatusName || '—' }} → {{ log.toStatusName }}
                </el-tag>
              </div>
              <div v-if="log.detail" class="muted">{{ log.detail }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>

      <div v-else class="panel detail-empty">
        <el-empty description="选择左侧事件查看详情，或由值班经理建立停射事件" :image-size="80" />
      </div>
    </div>

    <!-- 建立停射事件 -->
    <el-dialog v-model="createVisible" title="建立停射事件" width="560px" :close-on-click-modal="false">
      <el-form label-width="88px">
        <el-form-item label="事件类型">
          <el-select v-model="createForm.eventType" style="width: 100%">
            <el-option v-for="t in options.eventTypes || []" :key="t.code" :label="t.name" :value="t.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重级别">
          <el-radio-group v-model="createForm.severity">
            <el-radio-button v-for="s in options.severities || []" :key="s.code" :value="s.code">{{ s.name }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="停射原因">
          <el-input v-model="createForm.reason" maxlength="128" show-word-limit placeholder="一句话说明停射原因" />
        </el-form-item>
        <el-form-item label="现场说明">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            maxlength="512"
            show-word-limit
            placeholder="现场情况、已采取的临时措施等"
          />
        </el-form-item>
        <el-form-item label="受影响箭道">
          <el-select v-model="createForm.laneIds" multiple collapse-tags style="width: 100%" placeholder="选择要安全锁定的箭道">
            <el-option
              v-for="l in lanes"
              :key="l.id"
              :label="`${l.laneNo} · ${l.distance}m · ${l.statusName}`"
              :value="l.id"
              :disabled="l.status === 'LOCKED'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="受影响器材">
          <el-select v-model="createForm.equipmentIds" multiple collapse-tags style="width: 100%" placeholder="选择要冻结的器材">
            <el-option
              v-for="e in equips"
              :key="e.id"
              :label="`${e.equipCode} · ${e.typeName} · ${e.statusName}`"
              :value="e.id"
              :disabled="e.status === 'LOCKED'"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="dlg-warn">
        事件建立后立即生效：所选箭道安全锁定、其上进行中的回合暂停、所选器材冻结；锁定前状态会快照保存，放行时整体恢复。
      </div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" :disabled="acting" @click="submitCreate">建立并立即联锁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { equipApi, laneApi, safetyApi, shortTime } from '../api'

const ROLE_KEY = 'safety-role'
const NAME_KEY = 'safety-operator'
const PICK_KEY = 'safety-event-id'

const options = ref({})
const events = ref([])
const detail = ref(null)
const lanes = ref([])
const equips = ref([])
const currentId = ref(null)
const filterStatus = ref(null)
const acting = ref(false)
const createVisible = ref(false)

const role = ref(localStorage.getItem(ROLE_KEY) || 'MANAGER')
const operatorName = ref(localStorage.getItem(NAME_KEY) || '')

const reviewTypes = [
  { code: 'COACH', name: '教练复核' },
  { code: 'KEEPER', name: '器材管理员复核' }
]

const reviewForms = reactive({
  COACH: { conclusion: 'PASS', note: '' },
  KEEPER: { conclusion: 'PASS', note: '' }
})

const createForm = reactive({
  eventType: 'LANE_FAULT',
  severity: 'HIGH',
  reason: '',
  description: '',
  laneIds: [],
  equipmentIds: []
})

const isManager = computed(() => role.value === 'MANAGER')
const activeCount = computed(() => events.value.filter((e) => e.status !== 'RELEASED').length)
const isCreator = computed(() => !!detail.value && operatorName.value.trim() === detail.value.event.createdBy)

function reviewOf(type) {
  return detail.value ? detail.value.reviews.find((r) => r.reviewType === type) : null
}

const bothPassed = computed(() => {
  const coach = reviewOf('COACH')
  const keeper = reviewOf('KEEPER')
  return !!coach && !!keeper && coach.conclusion === 'PASS' && keeper.conclusion === 'PASS'
})

const anyRejected = computed(() => {
  const coach = reviewOf('COACH')
  const keeper = reviewOf('KEEPER')
  return (coach && coach.conclusion === 'REJECT') || (keeper && keeper.conclusion === 'REJECT')
})

/** 当前角色是否可对某个复核项签字：角色匹配 + 分项复核阶段 + 尚无结论 + 非创建人 */
function canReview(type) {
  return !!detail.value
    && role.value === type
    && detail.value.event.status === 'REVIEWING'
    && !reviewOf(type)
    && !!operatorName.value.trim()
    && !isCreator.value
}

function statusTagType(status) {
  if (status === 'RELEASED') return 'success'
  if (status === 'REVIEWING') return 'primary'
  return 'warning'
}

function severityTagType(severity) {
  if (severity === 'HIGH') return 'danger'
  if (severity === 'MEDIUM') return 'warning'
  return 'info'
}

function actionName(action) {
  return {
    CREATE: '建立停射事件',
    ADVANCE: '转入分项复核',
    REVIEW_COACH: '教练复核',
    REVIEW_KEEPER: '器材管理员复核',
    RELEASE: '复射放行'
  }[action] || action
}

watch(role, (value) => localStorage.setItem(ROLE_KEY, value))
watch(operatorName, (value) => localStorage.setItem(NAME_KEY, value))

async function loadEvents() {
  const params = {}
  if (filterStatus.value) params.status = filterStatus.value
  events.value = await safetyApi.list(params)
}

async function loadResources() {
  lanes.value = await laneApi.list()
  equips.value = await equipApi.list()
}

async function selectEvent(id) {
  currentId.value = id
  localStorage.setItem(PICK_KEY, String(id))
  detail.value = await safetyApi.detail(id)
}

async function refreshDetail() {
  if (currentId.value == null) return
  detail.value = await safetyApi.detail(currentId.value)
}

async function reloadAll() {
  await Promise.all([loadEvents(), loadResources()])
  if (currentId.value != null && events.value.some((e) => e.id === currentId.value)) {
    await refreshDetail()
  } else if (events.value.length) {
    await selectEvent(events.value[0].id)
  } else {
    currentId.value = null
    detail.value = null
  }
}

function openCreate() {
  if (!operatorName.value.trim()) {
    ElMessage.warning('请先填写操作人姓名（值班经理）')
    return
  }
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.reason.trim()) {
    ElMessage.warning('请填写停射原因')
    return
  }
  if (!createForm.description.trim()) {
    ElMessage.warning('请填写现场说明')
    return
  }
  if (!createForm.laneIds.length && !createForm.equipmentIds.length) {
    ElMessage.warning('请至少选择一条受影响箭道或一件受影响器材')
    return
  }
  acting.value = true
  try {
    const created = await safetyApi.create({
      operatorName: operatorName.value.trim(),
      operatorRole: role.value,
      eventType: createForm.eventType,
      severity: createForm.severity,
      reason: createForm.reason.trim(),
      description: createForm.description.trim(),
      laneIds: createForm.laneIds,
      equipmentIds: createForm.equipmentIds
    })
    ElMessage.success('停射事件已建立，受影响资源已安全锁定')
    createVisible.value = false
    createForm.reason = ''
    createForm.description = ''
    createForm.laneIds = []
    createForm.equipmentIds = []
    await reloadAll()
    await selectEvent(created.event.id)
  } finally {
    acting.value = false
  }
}

async function doAdvance() {
  acting.value = true
  try {
    detail.value = await safetyApi.advance(currentId.value, {
      operatorName: operatorName.value.trim(),
      operatorRole: role.value
    })
    ElMessage.success('已转入分项复核')
    await loadEvents()
  } finally {
    acting.value = false
  }
}

async function doReview(type) {
  const form = reviewForms[type]
  if (form.conclusion === 'REJECT' && !form.note.trim()) {
    ElMessage.warning('复核不通过时必须填写原因')
    return
  }
  acting.value = true
  try {
    detail.value = await safetyApi.review(currentId.value, {
      operatorName: operatorName.value.trim(),
      operatorRole: role.value,
      conclusion: form.conclusion,
      note: form.note.trim()
    })
    ElMessage.success('复核结论已记录')
    form.note = ''
    await loadEvents()
  } finally {
    acting.value = false
  }
}

async function doRelease() {
  try {
    await ElMessageBox.confirm(
      '确认两项复核均已通过，执行复射放行？受影响箭道、回合与器材将整体恢复至锁定前状态。',
      '复射放行',
      { type: 'warning', confirmButtonText: '确认放行', cancelButtonText: '再想想' }
    )
  } catch (e) {
    return
  }
  acting.value = true
  try {
    detail.value = await safetyApi.release(currentId.value, {
      operatorName: operatorName.value.trim(),
      operatorRole: role.value
    })
    ElMessage.success('已复射放行，受影响资源恢复至锁定前状态')
    await Promise.all([loadEvents(), loadResources()])
  } finally {
    acting.value = false
  }
}

onMounted(async () => {
  options.value = await safetyApi.options()
  const saved = Number(localStorage.getItem(PICK_KEY))
  currentId.value = Number.isFinite(saved) && saved > 0 ? saved : null
  await reloadAll()
})
</script>

<style scoped>
.interlock {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 14px;
  align-items: start;
}

.list-panel {
  position: sticky;
  top: 0;
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 66vh;
  overflow-y: auto;
}

.event-card {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.event-card:hover {
  border-color: var(--el-color-primary-light-5);
}

.event-card.is-on {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

.event-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.event-no {
  font-size: 12px;
  font-weight: 700;
}

.event-card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 13px;
}

.event-card-sub {
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-card-foot {
  margin-top: 4px;
  font-size: 11px;
}

.detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.detail .panel {
  margin: 0;
}

.detail-empty {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kv-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 18px;
}

.kv {
  display: flex;
  gap: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.kv-k {
  flex: 0 0 60px;
  color: var(--ink-mute);
  font-size: 12px;
}

.kv-v {
  flex: 1;
  word-break: break-all;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
}

.reject-tip {
  margin-top: 10px;
  padding: 8px 10px;
  font-size: 12px;
  color: #b3261e;
  background: #fdeceb;
  border-left: 3px solid #e57373;
  border-radius: 4px;
}

.res-groups {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.res-group {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 10px 12px;
  background: #fbfdfd;
}

.res-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.res-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 4px 0;
  font-size: 12px;
  border-top: 1px dashed var(--line);
}

.res-item:first-of-type {
  border-top: 0;
}

.review-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.review-card {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 10px 12px;
  background: #fbfdfd;
}

.review-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.review-result {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.review-note {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ink-soft);
}

.review-pending {
  padding: 4px 0;
}

.review-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
  margin-top: 6px;
}

.trail {
  padding-left: 4px;
}

.trail-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
}

.dlg-warn {
  margin-top: 4px;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.6;
  color: #8a4b00;
  background: #fff6e5;
  border-left: 3px solid #f0a63c;
  border-radius: 4px;
}

@media (max-width: 1100px) {
  .interlock {
    grid-template-columns: 1fr;
  }

  .res-groups,
  .review-grid,
  .kv-grid {
    grid-template-columns: 1fr;
  }
}
</style>
